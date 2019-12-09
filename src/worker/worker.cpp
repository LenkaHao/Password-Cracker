#include "worker.h"
#include "MD5.cpp"

Worker::Worker(const uint32_t port) {
  std::cout << "Server: initializing a server instance ..." << std::endl;
  std::cout << "Server: collecting server information ..." << std::endl;
  Worker::state = INIT;
  memset(&worker_addr, 0, sizeof(worker_addr));
  gethostname(hostname, MAXHOSTNAME);
  std::cout << "host: " << hostname << std::endl;
  hostent *hp;
  if ((hp = gethostbyname(hostname)) == NULL) {
    throw std::invalid_argument("Server: can not get host info");
  }
  worker_addr.sin_family = hp->h_addrtype;
  worker_addr.sin_addr.s_addr = htonl(INADDR_ANY);
  worker_addr.sin_port = htons(port);
  std::string worker_ip;
  std::string worker_port;
  parseSocket(&worker_addr, worker_ip, worker_port);
  std::cout << "Worker: Host " << worker_ip << " Port " << worker_port << std::endl;
}

int Worker::establish() {
  if ((worker_sockfd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
    perror("Worker: failed to create a socket");
    return -1;
  }
  std::cout << "Worker: open a socket successfully ..." << std::endl;
  if (bind(worker_sockfd, (struct sockaddr *)&worker_addr, sizeof(worker_addr)) < 0) {
    close(worker_sockfd);
    perror("Worker: failed to bind address with socket");
    return -1;
  }
  std::cout << "Worker: bind address to socket successfully ..." << std::endl;
  if ((listen(worker_sockfd, 3)) < 0 ) {
    close(worker_sockfd);
    perror("Worker: failed to listen the socket");
    return -1;
  }
  std::cout << "Worker: waiting for a connection request from clients ..." << std::endl;
  return 0;
}

int receiveAll(int sockfd, std::string &msg) {
  char buff[SO_RCVBUF + 1];
  while (msg.find(ENDMSG) == std::string::npos) {
    memset(buff, 0, sizeof(buff));    // must be init first before receiving
    int state = recv(sockfd, buff, SO_RCVBUF, 0);
    if (state < 0) {
      perror("Worker: failed to receive the echo message from master, closing socket ...");
      close(sockfd);
      return -1;
    } else if (state == 0) {
      std::cout << "Worker: detected master closed, closing socket ..." << std::endl;
      close(sockfd);
      return -1;
    }
    std::string tmp(buff);
    msg.append(tmp);
  }
  std::cout << "Worker received from master: " << msg << std::endl;
  return 0;
}

int sendAll(int sockfd, const std::string &msg, uint32_t size) {
  uint32_t total_sent = 0;
  uint32_t sent = 0;
  while (total_sent < size) {
    if ((sent = send(sockfd, msg.c_str() + total_sent, size - total_sent, 0)) < 0) {
      perror("Worker: failed to send the message to master, closing socket ...");
      close(sockfd);
      return  -1;
    }
    total_sent += sent;
  }
  std::cout << "Worker echo to master: " << msg << std::endl;
  return 0;
}

void echo(int csockfd, uint32_t id, std::map<uint32_t, std::string> *map) {
  std::string msg;
  while (true) {
    msg.clear();
    if (receiveAll(csockfd, msg) < 0) break;
    if (sendAll(csockfd, msg, msg.length()) < 0) break;
  }
  mtx.lock();
  map->erase(id);
  mtx.unlock();
  std::cout << "Worker: thread " << id << " ends" << std::endl;
}

void crack(int sockfd, Task *task, Worker *worker) {
  std::string pwd_str = task->start_pwd;
  char alphabet[26] = {'a','b','c','d','e','f','g','h','i','j','k','l',
                       'm','n','o','p','q','r','s','t','u','v','w','x','y','z'};
  for (int count = 0; count < task->range; ++count) {
    // TODO: brute force cracking

  }
  if (task->pwd_md5 == GetMD5String(pwd_str)) {
    sendAll(sockfd, pwd_str, pwd_str.length());
    mtx.lock();
    worker->setState(IDLE);
    mtx.unlock();
    std::cout << "Worker: cracking thread ends" << std::endl;
    return;
  }
  // Failed
  sendAll(sockfd, std::string("NULL"), 4);
  mtx.lock();
  worker->setState(IDLE);
  mtx.unlock();
  std::cout << "Worker: cracking thread ends" << std::endl;
}

bool parseTask(const std::string &msg, Task *task) {
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
  return i == 2;
}

int main(int argc, char *argv[]) {
  uint32_t port = DEFAULTPORT;
  if (argc >= 2) {
    char *tmp = argv[1];
    port = strtoul(tmp, nullptr, 10);
    if (port >= DEFAULTPORT && port <= 58999) {
      std::cout << "Worker: using port: " << port << std::endl;
    } else {
      std::cout << "Worker: invalid port number, use number between: 58000 - 58999  "<<std::endl;
      exit(0);
    }
  }
  Worker worker(port);
//  std::map<uint32_t, std::string> clients;
  if (worker.establish() < 0 ) {
    std::cerr << "Worker: failed to establish a listening socket, terminating ..." << std::endl;
    exit(1);
  }
  int connect_fd;
  uint32_t master_id = 0;
  sockaddr_in master_addr;
  socklen_t slen = sizeof(master_addr);

  std::string master_ip;
  std::string master_port;
  // wait for a connection to occur on a socket
  connect_fd = accept(worker.getSocketFd(), (struct sockaddr *)&master_addr, &slen);
  if (connect_fd < 0) {
    worker.closeSocket();
    perror("Worker: can not accept a connection with master's request");
    exit(1);
  }
  parseSocket(&master_addr, master_ip, master_port);
  std::cout << "Worker: established a connection with master: " << master_ip << " " << master_port <<std::endl;
  worker.setState(READY);

  // wait for a cracking task
  Task task;
  std::string message;
  while (receiveAll(connect_fd, message) >= 0) {
    // parse mission into task
    parseTask(message, &task);
    mtx.lock();
    Status state = worker.getState();
    mtx.unlock();
    // halt current crack task
    if (state == CRACK) {
      std::cout << "Worker: interrupt current crack task " << std::endl;
      continue;
    }
    // or start a new crack task
    if (state == READY || state == IDLE) {
      std::cout << "Worker: starting a thread for a new crack task " << std::endl;
      mtx.lock();
      worker.setState(CRACK);
      mtx.unlock();
      std::thread t1(crack, connect_fd, &task, &worker);
      t1.detach();
    }
  }
}
