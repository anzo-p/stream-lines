FROM node:21.5.0

WORKDIR /usr/src/app

COPY build/ ./build/
COPY package*.json ./

RUN npm ci --omit=dev

EXPOSE 3000

CMD ["node", "-r", "dotenv/config", "build"]
