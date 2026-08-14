# --------------------------------------------------------------------------------------------
# Construction de l'application cliente, puis service des fichiers statiques par un serveur
# frontal qui achemine aussi les appels vers les deux instances de l'application.
# --------------------------------------------------------------------------------------------
FROM node:22-alpine AS build

WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run build

# --------------------------------------------------------------------------------------------
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist/browser /usr/share/nginx/html

EXPOSE 80
