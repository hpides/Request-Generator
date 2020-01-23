#include "de_hpi_tdgt_fibers_Fiber.h"
LPVOID g_lpFiber[FIBER_COUNT];
LPVOID primary_fiber = NULL;
int created_fibers = 0;
JNIEnv* environment;
jobject toRun;

JNIEXPORT void JNICALL Java_de_hpi_tdgt_fibers_Fiber_init_1fibers
(JNIEnv* env, jclass java_class) {
	setbuf(stdout, NULL);
	primary_fiber = ConvertThreadToFiber(NULL);
}
VOID __stdcall runFiber(LPVOID lpParameter) {
	start: 
	printf("Now calling the run method...\n");
	jclass runnableClass = environment->GetObjectClass(toRun);
	if (runnableClass == NULL) {
		fprintf(stderr, "runnable class was null!");
	}
	jmethodID methodId = environment->GetMethodID(runnableClass, "run", "()V");
	if (methodId == NULL) {
		fprintf(stderr, "runnable method was null!");
	}
	environment->CallObjectMethod(toRun, methodId);
	printf("Run method returned!\n");
	SwitchToFiber(primary_fiber);
	goto start;
}
fiber_param* parameter;
JNIEXPORT jint JNICALL Java_de_hpi_tdgt_fibers_Fiber_create_1fiber
(JNIEnv* env, jobject self) {
	parameter = (fiber_param*) malloc(sizeof(fiber_param));
	environment = env;
	//runFiber(parameter);
	g_lpFiber[created_fibers] = CreateFiber(0, runFiber, NULL);
	//runFiber(parameter);
	if (g_lpFiber[created_fibers] == NULL) {
		fprintf(stderr,"Could not create fiber!\n");
		exit(0);
	}
	//SwitchToFiber(g_lpFiber[created_fibers]);
	created_fibers++;
	return created_fibers - 1;
fail:

	exit(10);
}

JNIEXPORT void JNICALL Java_de_hpi_tdgt_fibers_Fiber_run_1fiber
(JNIEnv* env, jobject, jint id, jobject runnable) {
	printf("Switching to fiber %d represented by %p\n", id, g_lpFiber[id]);
	environment = env;
	toRun = runnable;
	SwitchToFiber(g_lpFiber[id]);
}