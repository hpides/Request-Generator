if [ -z ${tag+x} ];
 then tag="latest";
fi
docker build -t requestgenerator:${tag} ../ -f ./Dockerfile
if [ -z ${deploy+x} ];
  then echo "no push";
else
  docker tag requestgenerator:${tag} localhost:5000/requestgenerator:${tag}
  docker push localhost:5000/requestgenerator:${tag}
fi
