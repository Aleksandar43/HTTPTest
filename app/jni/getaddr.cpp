#include <getaddr.h>
#include <iostream>
#include <string>

//string getAddr(){
//    return "10.20.30.50";
//}

extern "C" {

jstring Java_com_sourcico_httptest_MainActivity_getAddressToSend(JNIEnv *env, jobject thisObject) {
    return env->NewStringUTF("88.77.66.55");
}

}