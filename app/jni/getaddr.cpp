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
    struct sockaddr_in6 *good_ipv6 = NULL;
    struct sockaddr_in *good_ipv4 = NULL;

    struct ifaddrs *address_list;
    getifaddrs(&address_list);

    __android_log_print(ANDROID_LOG_INFO, TAG, "Addresses from getifaddrs():");
    struct ifaddrs *ifap = address_list;
    while(ifap != NULL){
        char *name = ifap->ifa_name;
        struct sockaddr* sock = ifap->ifa_addr;
        if (sock != NULL) {
            if (sock->sa_family == AF_INET) {
                struct sockaddr_in *ip4 = (struct sockaddr_in *) sock;
                __android_log_print(ANDROID_LOG_INFO, TAG,"name=%s, sock->sa_family=%d, address=%s", name,sock->sa_family, inet_ntoa(ip4->sin_addr));

                unsigned int addr = htonl(ip4->sin_addr.s_addr);
                if(!good_ipv4 && addr != INADDR_LOOPBACK
                    && !(
                            (addr & 0xFF000000) == 0x0A000000           //Class A private
                            || (addr & 0xFFF00000) == 0xAC100000        //Class B private
                            || (addr & 0xFFFF0000) == 0xC0A80000        //Class C private
                    )
                ){
                    good_ipv4 = ip4;
                }
            } else if (sock->sa_family == AF_INET6) {
                struct sockaddr_in6 *ip6 = (struct sockaddr_in6 *) sock;
                char *string_addr = (char *) malloc(INET6_ADDRSTRLEN * sizeof(char));
                inet_ntop(AF_INET6, &ip6->sin6_addr, string_addr, INET6_ADDRSTRLEN);
                __android_log_print(ANDROID_LOG_INFO, TAG,"name=%s, sock->sa_family=%d, address=%s", name,sock->sa_family, string_addr);
                free(string_addr);

                //If an IPv6 address is available, that is from the global unicast range, also any address that has 0x0 for the first word is not a global unicast address.
                unsigned char *addr = ip6->sin6_addr.s6_addr;
                if(!good_ipv6 &&
                    ((addr[0] == 0x20 && addr[1] == 0x02) //2002::
                    || (addr[0] == 0x20 && addr[1] == 0x01 && addr[2] == 0x00 && addr[3] == 0x00) //2001:0::
                )){
                    good_ipv6 = ip6;
                    break; // No need to iterate further
                }
            } else {
                __android_log_print(ANDROID_LOG_INFO, TAG, "name=%s, sock->sa_family=%d", name, sock->sa_family);
            }
        } else {
            __android_log_print(ANDROID_LOG_INFO, TAG, "name=%s, sock==NULL", name);
        }
        ifap = ifap->ifa_next;
    }
    __android_log_print(ANDROID_LOG_INFO, TAG, "Addresses from getifaddrs() finish");

    char *ret;
    if(good_ipv6){
        ret = (char*) malloc(INET6_ADDRSTRLEN * sizeof(char));
        inet_ntop(AF_INET6, &good_ipv6->sin6_addr, ret, INET6_ADDRSTRLEN);
    } else if(good_ipv4){
        ret = (char*) malloc(INET_ADDRSTRLEN * sizeof(char));
        inet_ntop(AF_INET, &good_ipv4->sin_addr, ret, INET_ADDRSTRLEN);
    } else{
        ret = NULL;
    }

    freeifaddrs(address_list);
    
    return ret;
}

jstring Java_com_sourcico_httptest_MainActivity_getAddressToSend(JNIEnv *env, jobject thisObject) {
    char* s = get_address();
    if(s == NULL){
        return NULL;
    }
    return env->NewStringUTF(s);
}

}