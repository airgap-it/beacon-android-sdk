FROM androidsdk/android-30:latest

RUN mkdir /build
WORKDIR /build

# copy source
COPY . /build

# accept licences
#RUN echo y | $ANDROID_HOME/tools/bin/sdkmanager --update

# clean project
RUN /build/gradlew --project-dir /build clean

# build apk, exclude prod flavored unit tests
RUN /build/gradlew --project-dir /build :core:assembleProd

# copy release aar
RUN cp /build/core/build/outputs/aar/core-prod-release.aar android-core-release-unsigned.aar

CMD ["/build/gradlew", "test"]