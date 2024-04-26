#include <getaddr.h>
#include <iostream>
#include <cstring>

#include <sys/socket.h>
#include <netinet/in.h>
#include <ifaddrs.h>
#include <arpa/inet.h>

//string getAddr(){
//    return "10.20.30.50";
//}

extern "C" {
    
char* get_address(){
    struct ifaddrs *address_list;
    getifaddrs(&address_list);
    
    char* a = (char*)malloc(500);
    struct sockaddr* sock = address_list->ifa_addr;
    if(sock->sa_family == AF_INET){
        struct sockaddr_in* ip4 = (struct sockaddr_in*) sock;
        strcpy(a, inet_ntoa(ip4->sin_addr));
        //return a;
    } else if(sock->sa_family == AF_INET6){
        struct sockaddr_in6* ip6 = (struct sockaddr_in6*) sock;
        inet_ntop(AF_INET6, ip6, a, INET6_ADDRSTRLEN);
        //return a;
        
    } else{
        strcpy(a, "44.55.66.77");
    }
    
    freeifaddrs(address_list);
    
    return a;
}

jstring Java_com_sourcico_httptest_MainActivity_getAddressToSend(JNIEnv *env, jobject thisObject) {
    char* s = get_address();
    return env->NewStringUTF(s);
}

}