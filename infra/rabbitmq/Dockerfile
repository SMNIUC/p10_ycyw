# Broker de messages avec le relais STOMP active.
#
# C'est ce relais qui permet a l'application de deleguer la diffusion : une trame publiee par une
# instance revient a toutes les instances abonnees a la destination. Sans lui, chaque instance ne
# diffuserait qu'a ses propres connexions, et deux interlocuteurs connectes a deux instances
# differentes ne se verraient jamais (DA-11).
FROM rabbitmq:4-management-alpine

RUN rabbitmq-plugins enable --offline rabbitmq_stomp

EXPOSE 5672 15672 61613
