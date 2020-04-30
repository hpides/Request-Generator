if [ -z ${tag+x} ];
 then tag="latest";
fi
docker build -t requestgenerator:latest ../ -f ./Dockerfile
if [ -z ${deploy+x} ];
  then echo "no push";
else
  docker tag requestgenerator:latest localhost:5000/requestgenerator:${tag}
  docker push localhost:5000/requestgenerator:${tag}
fi
docker build -t mosquitto:latest ./ -f ./mosquitto_dockerfile
if [ -z ${deploy+x} ];
  then echo "no push";
else
  docker tag mosquitto:latest localhost:5000/mosquitto:${tag}
  docker push localhost:5000/mosquitto:${tag}
fi

