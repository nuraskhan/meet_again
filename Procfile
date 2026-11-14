release: java -cp "build/stage/migration/*" kz.nuraskhan.hangelo.migration.Migrate "$JDBC_DATABASE_URL" "$JDBC_DATABASE_USERNAME" "$JDBC_DATABASE_PASSWORD"
web: HANGELO_DB_URL="$JDBC_DATABASE_URL" HANGELO_DB_USER="$JDBC_DATABASE_USERNAME" HANGELO_DB_PASSWORD="$JDBC_DATABASE_PASSWORD" java $JAVA_OPTS -Dserver.port=$PORT -jar build/stage/hangelo.jar
