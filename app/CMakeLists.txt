# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/native-lib.cpp )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# configure import libs
set(opencv_DIR ${CMAKE_SOURCE_DIR}/../opencv)

add_library( objdetect SHARED IMPORTED )
set_target_properties( objdetect PROPERTIES IMPORTED_LOCATION ${opencv_DIR}/sdk/native/libs/${ANDROID_ABI}/libopencv_objdetect.so)

add_library( imgproc SHARED IMPORTED )
set_target_properties( imgproc PROPERTIES IMPORTED_LOCATION ${opencv_DIR}/sdk/native/libs/${ANDROID_ABI}/libopencv_imgproc.so )

add_library( video SHARED IMPORTED )
set_target_properties( video PROPERTIES IMPORTED_LOCATION ${opencv_DIR}/sdk/native/libs/${ANDROID_ABI}/libopencv_video.so )

add_library( cvcore SHARED IMPORTED )
set_target_properties( cvcore PROPERTIES IMPORTED_LOCATION ${opencv_DIR}/sdk/native/libs/${ANDROID_ABI}/libopencv_core.so )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       native-lib
                       -landroid

                       objdetect

                       imgproc

                       video

                       cvcore

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )

target_include_directories( native-lib PRIVATE
                           ${opencv_DIR}/sdk/native/jni/include)