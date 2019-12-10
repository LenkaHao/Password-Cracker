#include "worker.h"
#include "MD5.cpp"

Worker::Worker(const char *host, const uint32_t port) : hostname(host) {
  Worker::state = INIT;
  if (host == nullptr) {
    std::cout << "Worker: undefined hostname, using localhost ..." << std::endl;
    hostname = "localhost";
  }
  std::cout << "Worker: initializing a client instance ..." << std::endl;
  std::cout << "Worker: collecting host information ..." << std::endl;
  hostent *hp;
  if ((hp = gethostbyname(hostname)) == NULL) {
    throw std::invalid_argument("Client: failed to get host info");
  }
  memset(&worker_addr, 0, sizeof(worker_addr));
  memcpy((char *)&worker_addr.sin_addr, hp->h_addr,hp->h_length); /* set address */
  worker_addr.sin_family = hp->h_addrtype;
  worker_addr.sin_port = htons(port);
  std::string server_ip;
  std::string server_port;
  parseSocket(&worker_addr, server_ip, server_port);
  std::cout <<"Worker: "<< "Host " << server_ip << " Port " << server_port << std::endl;
}

int Worker::createConnection() {
  std::cout << "Worker: creating client socket ..." << std::endl;
  if ((worker_sockfd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
    perror("Worker: failed to create a socket");
    return -1;
  }
  std::cout << "Worker: socket starts connecting to host ..." << std::endl;
  if (connect(worker_sockfd, (struct sockaddr *)&worker_addr, sizeof(worker_addr)) < 0) {
    close(worker_sockfd);
    perror("Worker: socket failed to connect with host ...");
    return -1;
  }
  return 0;
}

int receiveAll(int sockfd, std::string &msg) {
  char buff[SO_RCVBUF + 1];
  while (msg.find(ENDMSG) == std::string::npos) {
    memset(buff, 0, sizeof(buff));    // must be init first before receiving
    int state = recv(sockfd, buff, SO_RCVBUF, 0);
    if (state < 0) {
      perror("Worker: failed to receive messages from master, closing socket ...");
      return -1;
    } else if (state == 0) {
      std::cout << "Worker: detected server closed, closing socket ..." << std::endl;
      return -1;
    }
    std::string tmp(buff);
    msg.append(tmp);
  }
  std::cout << "Worker: received message: " << msg << std::endl;
  return 0;
}

int sendAll(int sockfd, const std::string &msg, uint32_t size) {
  uint32_t total_sent = 0;
  uint32_t sent = 0;
  while (total_sent < size) {
    if ((sent = send(sockfd, msg.c_str() + total_sent, size - total_sent, 0)) < 0) {
      return  -1;
    }
    total_sent += sent;
  }
  std::cout << "Worker: sent message: " << msg.c_str() << std::endl;    // already with '\n'
  return 0;
}

bool parseTask(const std::string &msg, Task *task) {
  if (msg == "QUIT\n") return true;
  // md5:32char, space:1char, pwd:5char, space:1char, range:>=1char;
  if(msg.length() < 40) return false;
  std::stringstream ss(msg);
  std::string str;
  int i = 0;
  while (ss >> str) {
    switch (i) {
      case 0:
        task->pwd_md5 = str;
        break;
      case 1:
        task->start_pwd = str;
        break;
      case 2:
        task->range = std::stoul(str);
        break;
      default:
        return false;
    }
    ++i;
  }
  return i == 3;
}

std::string nextPermutation(const std::string &pwd) {
  char pwd_arr[5 + 1] = "00000";
  bool carry = false;
  for (int i = pwd.length() - 1; i >= 0; --i) {
    if (!carry && i < pwd.length() - 1) {
      pwd_arr[i] = pwd.at(i);
      continue;
    }
    carry = nextASCII(pwd.at(i), &pwd_arr[i]);
  }
  std::string pwd_str(pwd_arr);
  return pwd_str;
}

void crack(int sockfd, Task *task, Worker *worker) {
  std::string end_str = "zzzzz";
  std::string pwd_str = task->start_pwd;
  int count = 0;
  while (pwd_str != end_str && count < task->range) {
    mtx.lock();
    Status state = worker->getState();
    mtx.unlock();
    if (state == TERMINATE) {
      std::cout << "Worker: STOP CRACKING! cracking thread ends ..." << std::endl;
      return;
    }
    if (task->pwd_md5 == GetMD5String(pwd_str)) {
      std::string snd_pwd = pwd_str;
      snd_pwd.append(ENDMSG);
      sendAll(sockfd, snd_pwd, snd_pwd.length());
      mtx.lock();
      worker->setState(IDLE);
      mtx.unlock();
      std::cout << "Worker: PWD FOUND! cracking thread ends ..." << std::endl;
      return;
    }
    pwd_str = nextPermutation(pwd_str);
    ++count;
  }
  // Failed
  sendAll(sockfd, std::string("00000\n"), 6);
  mtx.lock();
  worker->setState(IDLE);
  mtx.unlock();
  std::cout << "Worker: PWD NOT FOUND! cracking thread ends ..." << std::endl;
}

int main(int argc, char *argv[]) {
  char *host = nullptr;
  uint32_t port = DEFAULTPORT;
  if (argc >= 2) {
    host = argv[1];
    std::cout << "Worker: using host: " << host << std::endl;
    if (argc > 2) {
      char *tmp = argv[2];
      port = strtoul(tmp, nullptr, 10);
      if (port >= DEFAULTPORT && port <= 58999) {
        std::cout << "Worker: using port: " << port << std::endl;
      } else {
        std::cout << "Worker: invalid port number, use number between: 58000 - 58999  "<<std::endl;
        exit(0);
      }
    }
  }
  Worker worker(host, port);
//  std::map<uint32_t, std::string> clients;
  // create socket and connect
  if (worker.createConnection() < 0) {
    worker.closeSocket();
    std::cerr << "Worker: terminating ..." << std::endl;
    exit(1);
  }
  std::cout << "Worker: connected with host successfully ..." << std::endl;
  worker.setState(READY);

  // wait for a cracking task
  Task task;
  std::string rcvd_msg;
  while (receiveAll(worker.getSocketFd(), rcvd_msg) >= 0) {
    std::string message = rcvd_msg;
    rcvd_msg.clear();
    mtx.lock();
    Status state = worker.getState();
    mtx.unlock();

    // parse mission into task
    bool qualified = parseTask(message, &task);
    if (!qualified)  {
      std::string exp_msg = "00000";
      exp_msg.append(ENDMSG);
      std::cout << "Worker: unqualified order " << std::endl;
      sendAll(worker.getSocketFd(), exp_msg, exp_msg.length());
      continue;
    }
    // halt current crack task
    if (message == "QUIT\n" && state == CRACK) {
      std::cout << "Worker: interrupt current crack task " << std::endl;
      mtx.lock();
      worker.setState(TERMINATE);
      mtx.unlock();
      continue;
    }
    // or start a new crack task
    if (state == READY || state == IDLE) {
      std::cout << "Worker: starting a thread for a new crack task " << std::endl;
      mtx.lock();
      worker.setState(CRACK);
      mtx.unlock();
      std::thread t1(crack, worker.getSocketFd(), &task, &worker);
      t1.detach();
    }
  }
}
