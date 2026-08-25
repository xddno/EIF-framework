# CMakeList.txt : CMake project for MetaJNI2, include source and define
# project specific logic here.
#
cmake_minimum_required (VERSION 3.20)

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

# Enable Hot Reload for MSVC compilers if supported.
if (POLICY CMP0141)
  cmake_policy(SET CMP0141 NEW)
  set(CMAKE_MSVC_DEBUG_INFORMATION_FORMAT "$<IF:$<AND:$<C_COMPILER_ID:MSVC>,$<CXX_COMPILER_ID:MSVC>>,$<$<CONFIG:Debug,RelWithDebInfo>:EditAndContinue>,$<$<CONFIG:Debug,RelWithDebInfo>:ProgramDatabase>>")
endif()

project("JarLoader")

set(MINECRAFT_CLASS "net/minecraft/client/Minecraft" CACHE STRING "Minecraft Class Name")

# Add source to this project's executable.
add_library(JarLoader SHARED "src/main.cpp" "src/jvmti/jvmti.cpp" "src/logger/logger.cpp" "src/transformer/transformer.cpp")

target_compile_definitions(JarLoader PUBLIC MINECRAFT_CLASS="${MINECRAFT_CLASS}")

target_include_directories(JarLoader PUBLIC "include" "include/jni")

target_link_directories(JarLoader PUBLIC "lib")
target_link_libraries(JarLoader PUBLIC "jvm")

set_property(TARGET JarLoader PROPERTY CXX_STANDARD 20)

if(MSVC)
	set_property(TARGET JarLoader PROPERTY MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
endif()

if(WIN32)
	target_include_directories(JarLoader PUBLIC "include/jni/win32")
	# target_link_libraries(JarLoader PUBLIC "WS2_32")
	# "CRYPT32" "shlwapi"
elseif(UNIX)
	target_include_directories(JarLoader PUBLIC "include/jni/linux")
	target_link_libraries(JarLoader PUBLIC "X11") # apt install libx11-dev
endif()

# TODO: Add tests and install targets if needed.
