//
// Created by Guanting on 2019/12/9.
//

#ifndef WORKER_WORKER_H
#define WORKER_WORKER_H
#include <netdb.h>
#include <cstdio>
#include <cstring>
#include <iostream>
#include <sstream>
#include <climits>
#include <map>
#include <mutex>
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <thread>


#define DEFAULTPORT 9000
#define MAXHOSTNAME 255
#define ENDMSG "\n"

enum Status{INIT, IDLE, CRACK, TERMINATE};

struct Task {
  std::string pwd_md5;
  std::string start_pwd;
  uint16_t range = 0;
};

class Worker {
public:
  Worker() : Worker("localhost", DEFAULTPORT) {
    std::cout << "Worker: undefined host name, using localhost with port: 58000" << std::endl;
  }
  Worker(const char *host, const uint32_t port);
  int createConnection();
  sockaddr_in getSocketAddress() const { return worker_addr; }
  int getSocketFd() const { return worker_sockfd; }
  void closeSocket() { close(worker_sockfd); }
  void setState(Status s) { state = s ;}
  Status getState() { return state; }

private:
  int worker_sockfd;  // 3: listening socket
  sockaddr_in worker_addr;
  const char *hostname;
  Status state;
};

inline void parseSocket(sockaddr_in *socket_addr, std::string &ip, std::string &port) {
  ip = inet_ntoa(socket_addr->sin_addr);
  port = std::to_string(ntohs(socket_addr->sin_port));
}

inline void parseTask(sockaddr_in *socket_addr, std::string &ip, std::string &port) {
  ip = inet_ntoa(socket_addr->sin_addr);
  port = std::to_string(ntohs(socket_addr->sin_port));
}

// return true if increase lead to a carry
inline bool nextASCII(char in, char *out) {
  // ASCII A-Z [65-90], a-z[97,122]
  if (static_cast<int>(in) == 90) {
    *out = static_cast<char>(97);
    return false;
  } else if (static_cast<int>(in) == 122) {
    *out = static_cast<char>(65);
    return true;
  } else {
    *out = static_cast<char>(static_cast<int>(in) + 1);
    return false;
  }
}

std::mutex mtx;
void crack(int sockfd, Task *task, Worker *worker);
int sendAll(int sockfd, const std::string &msg, uint32_t size);
int receiveAll(int sockfd, std::string &msg);
bool parseTask(const std::string &msg, Task *task);
std::string nextPermutation(const std::string &pwd);
std::string nextPartition(const std::string &head, uint32_t size);
#endif //WORKER_WORKER_H
