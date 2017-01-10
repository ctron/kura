# Build native libaries

This projects holds a few scripts which perform the compilation and cross-compilation
of the native binaries which Eclipse Kura requires.

In order to perform a full build you will need:

* A 64bit Linux
* Docker installed
* Access to the internet
* GNU Make install
* Root access to your machine

Run the following command:

    sudo make

This will create a set of docker images and afterwards run the build scripts inside those
docker images.

The result will be placed in the output folder `output`.

## Native targets

<dl>
<dt>rhel7/x86_64</dt><dd>Plain RHEL7/CentOS7</dd>
<dt>armv7hf</dt><dd>ARM v7 hard-float, `libudev.so.1`</dd>
</dl>