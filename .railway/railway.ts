import { defineRailway, project, service } from "railway/iac";

// Configuracion del servicio multimetrico-api en Railway. Railway no lee este archivo
// al desplegar: se aplica a mano con `railway config plan` y
// `railway config apply`, y queda guardada en el servicio.
//
// Administra solo este servicio (partial): la MySQL compartida del proyecto
// demos, las otras APIs, los dominios y las variables siguen en el panel.
export const partial = "multimetrico-api";

export default defineRailway(() => {
  const api = service("multimetrico-api", {
    // Sin fuente a proposito: este servicio se sube con `railway up`.
    // Explicito: el servicio tiene guardado RAILPACK y, sin esto, el proximo
    // deploy volveria a la autodeteccion en lugar de la imagen del Dockerfile.
    build: { builder: "DOCKERFILE", dockerfilePath: "Dockerfile" },
    // La sonda de Actuator, sin detalles. Railway no da por bueno un deploy
    // hasta que responde UP.
    healthcheck: "/actuator/health",
    healthcheckTimeout: 180,
    deploy: {
      // La politica ya es ON_FAILURE por defecto (Railway la guarda vacia);
      // solo se acota la cantidad de reintentos.
      restartPolicyMaxRetries: 3,
    },
  });
  return project("demos", {
    // Las variables son secretos y se cargan en el panel: este archivo nunca
    // las lee ni las escribe.
    variables: { managed: false },
    resources: [api],
  });
});
