FROM androidsdk/android-29:latest

RUN mkdir /build
WORKDIR /build

# copy source
COPY . /build

# accept licences
#RUN echo y | $ANDROID_HOME/tools/bin/sdkmanager --update

# clean project
RUN /build/gradlew --project-dir /build clean

# build apk
RUN /build/gradlew --project-dir /build build

# copy release aar
RUN cp /build/app/build/outputs/aar/app-release.aar android-release-unsigned.aar

CMD ["/build/gradlew", "test"]