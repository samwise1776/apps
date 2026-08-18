FROM node:22-alpine AS build

WORKDIR /app
COPY package.json ./
COPY scripts ./scripts
COPY *.html *.css *.js *.webmanifest *.txt ./
COPY assets ./assets
COPY downloads ./downloads
RUN npm run build

FROM python:3.12-slim

WORKDIR /app
COPY database ./database
COPY --from=build /app/dist ./dist

ENV HOST=0.0.0.0 \
    PORT=8000 \
    DESKTOPCRAFT_SITE_ROOT=/app/dist \
    DESKTOPCRAFT_DB_PATH=/data/desktopcraft.db \
    DESKTOPCRAFT_FEEDBACK_FILE=/data/texta.txt \
    DESKTOPCRAFT_SECURE_COOKIES=true

VOLUME ["/data"]
EXPOSE 8000

CMD ["python3", "database/server.py", "--host", "0.0.0.0"]
