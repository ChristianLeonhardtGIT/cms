FROM node:24-alpine

WORKDIR /app
COPY package.json ./
COPY lib ./lib
COPY public ./public
COPY chos-reader ./chos-reader
COPY server.mjs admin.mjs ./

ENV NODE_ENV=production \
    BETA_PORT=3000 \
    BETA_DATA_DIR=/app/data

RUN addgroup -S portal && adduser -S -G portal portal \
    && mkdir -p /app/data \
    && chown -R portal:portal /app

USER portal
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD node -e "fetch('http://127.0.0.1:3000/beta/health').then((response) => { if (!response.ok) process.exit(1); }).catch(() => process.exit(1));"
CMD ["node", "server.mjs"]
