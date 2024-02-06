MAIN_CLASS = org.example.sqs.App

start-localstack:
	docker compose -f docker-compose.yml up -d
	aws sqs create-queue --queue-name example-queue --endpoint-url http://localhost:4566 --profile localstack

clean:
	docker compose -f docker-compose.yml down -v
	docker compose -f docker-compose.yml rm -s -f -v

package-and-run:
	mvn clean package
	mvn exec:java -Dexec.mainClass=org.example.sqs.App
