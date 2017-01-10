.PHONY: all clean images build

PROJECTS=org.eclipse.kura.linux.usb

all: clean images build

clean:
	-rm -Rf build
	-rm -Rf output

prepare:
	mkdir -p output
	mkdir -p build/kura
	
	rsync -rv \
		--exclude=*.so \
		--exclude=*.o \
		--exclude=*/target \
		../kura/org.eclipse.kura.linux.usb build/kura/

images: prepare
	docker build -t kura-native-build-fedora25  -f fedora25/Dockerfile .
	docker build -t kura-native-build-rhel7     -f rhel7/Dockerfile .
	docker build -t kura-native-build-ubuntu    -f ubuntu/Dockerfile .

build:
	docker run -v $(CURDIR)/output:/output:z kura-native-build-fedora25
	docker run -v $(CURDIR)/output:/output:z kura-native-build-rhel7
	docker run -v $(CURDIR)/output:/output:z kura-native-build-ubuntu

update:
	cp output/fedora25/armv7hf/libEurotechUdev.so  ../kura/org.eclipse.kura.linux.usb.x86.armv7hf/lib/linux/
	cp output/rhel7/x86_64/libEurotechUdev.so      ../kura/org.eclipse.kura.linux.usb.x86_64.rhel7/lib/linux/
	cp output/ubuntu/x86/libEurotechUdev.so        ../kura/org.eclipse.kura.linux.usb.x86.ubuntu/lib/linux/
	cp output/rhel7/x86_64/libEurotechUdev.so      ../kura/org.eclipse.kura.linux.usb.x86_64/lib/linux/
	cp output/ubuntu/x86/libEurotechUdev.so        ../kura/org.eclipse.kura.linux.usb.x86/lib/linux/
