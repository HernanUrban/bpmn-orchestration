# WARNING: update the "version" file when upgrading this version
FROM camunda/camunda-bpm-platform:run-7.17.0

# Custom delegates
COPY dependencies/ /camunda/configuration/userlib/
COPY build/libs/*.jar /camunda/configuration/userlib/

COPY configuration/production.yml /camunda/configuration/production.yml
# override default config to avoid accidental misconfigurations 
COPY configuration/production.yml /camunda/configuration/default.yml

CMD ["./camunda.sh", "--production"]
