# ── Estágio 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copia arquivos de dependência primeiro (aproveita cache do Docker)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Garante permissão de execução no Maven Wrapper
RUN chmod +x mvnw

# Baixa dependências sem compilar o código
RUN ./mvnw dependency:go-offline -q

# Copia o código-fonte e compila (pula os testes)
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ── Estágio 2: Imagem final (leve) ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copia somente o JAR gerado
COPY --from=builder /app/target/*.jar app.jar

# Porta padrão do Spring Boot (Railway detecta automaticamente)
EXPOSE 8080

# Inicia com o perfil de produção
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]