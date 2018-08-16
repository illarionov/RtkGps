For building I used:

Build platform: MacOS 10.13.6

gdal 2.3.1
I installed the latest Oracle JDK
With brew I installed swig

1- I built the 4 needed standalone NDK toolchains with:
PLATFORM=21
~/Library/Android/sdk/ndk-bundle/build/tools/make-standalone-toolchain.sh --verbose --force --platform=android-$PLATFORM --use-llvm --stl=libc++ --install-dir=/Users/rlemeill/Development/android-toolchain/x86_64 --arch=x86_64
~/Library/Android/sdk/ndk-bundle/build/tools/make-standalone-toolchain.sh --verbose --force --platform=android-$PLATFORM --use-llvm --stl=libc++ --install-dir=/Users/rlemeill/Development/android-toolchain/x86 --arch=x86
~/Library/Android/sdk/ndk-bundle/build/tools/make-standalone-toolchain.sh --verbose --force --platform=android-$PLATFORM --use-llvm --stl=libc++ --install-dir=/Users/rlemeill/Development/android-toolchain/arm64 --arch=arm64
~/Library/Android/sdk/ndk-bundle/build/tools/make-standalone-toolchain.sh --verbose --force --platform=android-$PLATFORM --use-llvm --stl=libc++ --install-dir=/Users/rlemeill/Development/android-toolchain/arm --arch=arm

2- I set the path with
export PATH=$PATH:~/Development/android-toolchain/arm/bin/:~/Development/android-toolchain/arm64/bin/:~/Development/android-toolchain/x86/bin/:~/Development/android-toolchain/x86_64/bin/

3- I add fake_std.c in port directory
3b- I added fake_std.o to the list of objects in port/GNUMakefile

4- I changed org/gdal/gdal/ColorTable.java because of awt lacks in Android

5- I build all the 4 platforms with 
make distclean
./20-BUILD x86
make distclean
./20-BUILD x86_64
make distclean
./20-BUILD arm
make distclean
./20-BUILD arm64

sometimes I needed to replay the creation of the .a and the .so . I don't know why 
but make use local ar and not x86_64_linux_android-ar for example
take care of variables settings before copy-pasting lines from bash scripts

