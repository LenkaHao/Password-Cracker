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


#define DEFAULTPORT 58000
#define MAXHOSTNAME 255
#define ENDMSG "\n"

enum Status{INIT, READY, IDLE, CRACK};

struct Task {
  std::string pwd_md5;
  std::string start_pwd;
  uint16_t range = 0;
};

class Worker {
public:
  Worker() : Worker(DEFAULTPORT) {
    std::cout << "Worker: undefined port number name, using 58000" << std::endl;
  }
  Worker(const uint32_t port);
  int establish();
  sockaddr_in getSocketAddress() const { return worker_addr; }
  int getSocketFd() const { return worker_sockfd; }
  void closeSocket() { close(worker_sockfd); }
  void setState(Status s) { state = s ;}
  Status getState() { return state; }

private:
  int worker_sockfd;  // 3: listening socket
  sockaddr_in worker_addr;
  char hostname[MAXHOSTNAME + 1];
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


std::mutex mtx;
//void echo(int sockfd, uint32_t id, std::map<uint32_t, std::string> *map);
void crack(int sockfd, Task *task, Worker *worker);
int sendAll(int sockfd, const std::string &msg, uint32_t size);
int receiveAll(int sockfd, std::string &msg);
bool parseTask(const std::string &msg, Task *task);
#endif //WORKER_WORKER_H
