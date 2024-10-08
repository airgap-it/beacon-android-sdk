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
RUN /build/gradlew --project-dir /build :client-wallet:assembleProd
RUN /build/gradlew --project-dir /build :client-wallet-compat:assembleProd
RUN /build/gradlew --project-dir /build :blockchain-substrate:assembleProd
RUN /build/gradlew --project-dir /build :blockchain-tezos:assembleProd
RUN /build/gradlew --project-dir /build :transport-p2p-matrix:assembleProd

# copy release aar
RUN cp /build/core/build/outputs/aar/core-prod-release.aar android-core-release-unsigned.aar
RUN cp /build/client-wallet/build/outputs/aar/client-wallet-prod-release.aar android-client-wallet-release-unsigned.aar
RUN cp /build/client-wallet-compat/build/outputs/aar/client-wallet-compat-prod-release.aar android-client-wallet-compat-release-unsigned.aar
RUN cp /build/blockchain-substrate/build/outputs/aar/blockchain-substrate-prod-release.aar android-blockchain-substrate-release-unsigned.aar
RUN cp /build/blockchain-tezos/build/outputs/aar/blockchain-tezos-prod-release.aar android-blockchain-tezos-release-unsigned.aar
RUN cp /build/transport-p2p-matrix/build/outputs/aar/transport-p2p-matrix-prod-release.aar android-transport-p2p-matrix-release-unsigned.aar

CMD ["/build/gradlew", "test"]