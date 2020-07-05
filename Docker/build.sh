#
# WALT - A realistic load generator for web applications.
#
# Copyright 2020 Eric Ackermann <eric.ackermann@student.hpi.de>, Hendrik Bomhardt
# <hendrik.bomhardt@student.hpi.de>, Benito Buchheim
# <benito.buchheim@student.hpi.de>, Juergen Schlossbauer
# <juergen.schlossbauer@student.hpi.de>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
if [ -z ${tag+x} ];
 then tag="latest";
fi
docker build -t worldofjarcraft/requestgenerator:latest ../ -f ./Dockerfile
if [ -z ${deploy+x} ];
  then echo "no push";
else
  docker tag worldofjarcraft/requestgenerator:latest ${REGISTRY}/requestgenerator:${tag}
  docker push ${REGISTRY}/requestgenerator:${tag}
fi
docker build -t  worldofjarcraft/mosquitto:latest ./ -f ./mosquitto_dockerfile
if [ -z ${deploy+x} ];
  then echo "no push";
else
  docker tag  worldofjarcraft/mosquitto:latest ${REGISTRY}/mosquitto:${tag}
  docker push ${REGISTRY}/mosquitto:${tag}
fi

