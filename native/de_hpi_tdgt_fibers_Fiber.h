#include <jni.h>
#include <stdio.h>
#include <Windows.h>
/* Header for class de_hpi_tdgt_fibers_Fiber */

#ifndef _Included_de_hpi_tdgt_fibers_Fiber
#define _Included_de_hpi_tdgt_fibers_Fiber
#ifdef __cplusplus
#define FIBER_COUNT 100000
extern "C" {
#endif

    typedef struct fiber_param {
        jobject runnable;
        jmethodID methodToCall;
    } fiber_param;

    /*
     * Class:     de_hpi_tdgt_fibers_Fiber
     * Method:    init_fibers
     * Signature: ()V
     */
    JNIEXPORT void JNICALL Java_de_hpi_tdgt_fibers_Fiber_init_1fibers
    (JNIEnv*, jclass);

    /*
    * Class:     de_hpi_tdgt_fibers_Fiber
    * Method:    create_fiber
    * Signature: (Ljava/lang/Runnable;)I
    */
    JNIEXPORT jint JNICALL Java_de_hpi_tdgt_fibers_Fiber_create_1fiber
    (JNIEnv*, jobject);

    /*
     * Class:     de_hpi_tdgt_fibers_Fiber
     * Method:    run_fiber
     * Signature: (I)V
     */
    JNIEXPORT void JNICALL Java_de_hpi_tdgt_fibers_Fiber_run_1fiber
    (JNIEnv*, jobject, jint, jobject);

#ifdef __cplusplus
}
#endif
#endif
