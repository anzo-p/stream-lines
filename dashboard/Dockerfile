FROM node:20 as builder

WORKDIR /usr/src/app

COPY package*.json ./

RUN npm ci --omit=dev

COPY build/ ./build/

FROM node:21-alpine

WORKDIR /usr/src/app

COPY --from=builder /usr/src/app .

EXPOSE 3000

CMD ["node", "-r", "dotenv/config", "build"]
