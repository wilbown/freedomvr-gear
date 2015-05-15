LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include ../../../../vRLib/import_vrlib.mk		# import VRLib for this module.  Do NOT call $(CLEAR_VARS) until after building your module.
										# use += instead of := when defining the following variables: LOCAL_LDLIBS, LOCAL_CFLAGS, LOCAL_C_INCLUDES, LOCAL_STATIC_LIBRARIES 

include ../../../../vRLib/cflags.mk

LOCAL_MODULE    := vrscene
LOCAL_SRC_FILES  := VrScene.cpp

include $(BUILD_SHARED_LIBRARY)
