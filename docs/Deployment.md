Push repo to GitHub

Go to render.com

New → Web Service

Connect repo

Build command:

./gradlew build

Start command:

java -jar build/libs/*.jar

Result:

https://your-app.onrender.com

## Test dockerfile container works 
sudo docker build -t spring-test .
sudo docker run -p 8080:8080 spring-test
