release: java -cp "build/stage/migration/*" kz.nuraskhan.meet_again.migration.Migrate "$JDBC_DATABASE_URL" "$JDBC_DATABASE_USERNAME" "$JDBC_DATABASE_PASSWORD"
web: MAGAIN_DB_URL="$JDBC_DATABASE_URL" MAGAIN_DB_USER="$JDBC_DATABASE_USERNAME" MAGAIN_DB_PASSWORD="$JDBC_DATABASE_PASSWORD" java $JAVA_OPTS -Dserver.port=$PORT -jar build/stage/magain.jar
