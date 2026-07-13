FROM grafana/grafana:13.1.0

USER root

COPY provisioning /etc/grafana/provisioning
COPY dashboards /etc/grafana/dashboards

RUN chown -R grafana:root /etc/grafana/provisioning /etc/grafana/dashboards \
    && chmod -R a+rX /etc/grafana/provisioning /etc/grafana/dashboards

USER grafana
