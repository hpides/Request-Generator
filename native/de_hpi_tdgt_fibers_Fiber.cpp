#include "de_hpi_tdgt_fibers_Fiber.h"

JNIEXPORT void JNICALL Java_de_hpi_tdgt_fibers_Fiber_sayHello
  (JNIEnv* env, jobject thisObject) {
    std::cout << "Hello from C++ !!" << std::endl;
}