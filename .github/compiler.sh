#!/bin/bash

cd ~
sudo apt-get update -y
sudo apt-get upgrade -y
sudo apt-get install -y make git zlib1g-dev libssl-dev gperf cmake clang-10 libc++-dev libc++abi-dev
git clone --recursive https://github.com/tdlib/telegram-bot-api.git
rm -rf telegram-bot-api/build
mkdir -p telegram-bot-api/build
cd telegram-bot-api/build
CXXFLAGS="-stdlib=libc++" CC=/usr/bin/clang-10 CXX=/usr/bin/clang++-10 cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX:PATH=.. ..
cmake --build . --target install