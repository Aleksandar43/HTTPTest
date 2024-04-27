#include <getaddr.h>
#include <iostream>
#include <cstring>

#include <sys/socket.h>
#include <netinet/in.h>
#include <ifaddrs.h>
#include <arpa/inet.h>

#include <android/log.h>

extern "C" {

static const char* TAG = "HTTPTest";

char* get_address(){
    struct ifaddrs *address_list;
    getifaddrs(&address_list);

    __android_log_print(ANDROID_LOG_INFO, TAG, "Addresses from getifaddrs():");
    struct ifaddrs *s = address_list;
    while(s != NULL){
        char *name = s->ifa_name;
        struct sockaddr* sock = s->ifa_addr;
        if (sock != NULL) {
            int family = sock->sa_family;
            if (sock->sa_family == AF_INET) {
                struct sockaddr_in *ip4 = (struct sockaddr_in *) sock;
                __android_log_print(ANDROID_LOG_INFO, TAG,"name=%s, sock->sa_family=%d, address=%s", name,sock->sa_family, inet_ntoa(ip4->sin_addr));
            } else if (sock->sa_family == AF_INET6) {
                struct sockaddr_in6 *ip6 = (struct sockaddr_in6 *) sock;
                char *string_addr = (char *) malloc(INET6_ADDRSTRLEN * sizeof(char));
                inet_ntop(AF_INET6, &ip6->sin6_addr, string_addr, INET6_ADDRSTRLEN);
                __android_log_print(ANDROID_LOG_INFO, TAG,"name=%s, sock->sa_family=%d, address=%s", name,sock->sa_family, string_addr);
                free(string_addr);
            } else {
                __android_log_print(ANDROID_LOG_INFO, TAG, "name=%s, sock->sa_family=%d", name, sock->sa_family);
            }
        } else {
            __android_log_print(ANDROID_LOG_INFO, TAG, "name=%s, sock==NULL", name);
        }
        s = s->ifa_next;
    }
    __android_log_print(ANDROID_LOG_INFO, TAG, "Addresses from getifaddrs() finish");
    
    char* a = (char*)malloc(500);
    struct sockaddr* sock = address_list->ifa_addr;
    if(sock->sa_family == AF_INET){
        struct sockaddr_in* ip4 = (struct sockaddr_in*) sock;
        strcpy(a, inet_ntoa(ip4->sin_addr));
    } else if(sock->sa_family == AF_INET6){
        struct sockaddr_in6* ip6 = (struct sockaddr_in6*) sock;
        inet_ntop(AF_INET6, &ip6->sin6_addr, a, INET6_ADDRSTRLEN);

    } else{
        strcpy(a, "0.0.0.0");
    }
    
    freeifaddrs(address_list);
    
    return a;
}

jstring Java_com_sourcico_httptest_MainActivity_getAddressToSend(JNIEnv *env, jobject thisObject) {
    char* s = get_address();
    return env->NewStringUTF(s);
}

}