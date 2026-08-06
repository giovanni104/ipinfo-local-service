import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Métricas personalizadas
const successfulRequests = new Counter('successful_requests');
const failedRequests = new Counter('failed_requests');
const connectionErrors = new Counter('connection_errors');
const status429 = new Counter('status_429');
const status500 = new Counter('status_500');
const status503 = new Counter('status_503');
const unexpectedStatuses = new Counter('unexpected_statuses');

const functionalErrorRate = new Rate('functional_error_rate');
const slowResponseRate = new Rate('slow_response_rate');
const lookupDuration = new Trend('ip_lookup_duration', true);

// IP previamente verificadas con respuesta HTTP 200
const ips = [
  // Colombia
  '190.242.44.182',
  '181.49.10.10',
  '186.154.0.1',
  '190.85.0.1',
  '200.116.0.1',
  '181.57.0.1',
  '186.80.0.1',
  '190.144.0.1',

  // Estados Unidos y DNS públicos
  '8.8.8.8',
  '8.8.4.4',
  '4.2.2.1',
  '208.67.222.222',

  // Cloudflare y Quad9
  '1.1.1.1',
  '1.0.0.1',
  '9.9.9.9',

  // Europa
  '80.80.80.80',
  '193.0.6.139',

  // Asia y Oceanía
  '210.140.92.183',
  '202.12.27.33',

  // América Latina
  '200.160.2.3',
  '200.7.84.2',
  '190.211.254.1',
  '200.40.30.245',
];

export const options = {
  scenarios: {
    extreme_stress: {
      executor: 'ramping-arrival-rate',

      startRate: 500,
      timeUnit: '1s',

      preAllocatedVUs: 500,
      maxVUs: 3000,

      stages: [
        // Calentamiento
        { duration: '30s', target: 500 },

        // Carga moderada
        { duration: '1m', target: 1000 },
        { duration: '1m', target: 2000 },

        // Carga alta
        { duration: '1m', target: 4000 },
        { duration: '1m', target: 6000 },

        // Estrés severo
        { duration: '1m', target: 8000 },
        { duration: '1m', target: 10000 },

        // Punto máximo
        { duration: '1m', target: 15000 },
        { duration: '30s', target: 15000 },

        // Recuperación
        { duration: '30s', target: 5000 },
        { duration: '30s', target: 1000 },
        { duration: '30s', target: 0 },
      ],

      // Permite hasta un minuto para terminar solicitudes activas.
      gracefulStop: '1m',
    },
  },

  thresholds: {
    // Menos del 1 % de fallos HTTP o de conexión.
    http_req_failed: [
      {
        threshold: 'rate<0.01',
        abortOnFail: false,
      },
    ],

    // Objetivos de rendimiento.
    http_req_duration: [
      'p(90)<250',
      'p(95)<500',
      'p(99)<2000',
    ],

    ip_lookup_duration: [
      'p(95)<500',
      'p(99)<2000',
    ],

    // Aquí solo se consideran errores funcionales los estados distintos de 200.
    functional_error_rate: [
      {
        threshold: 'rate<0.01',
        abortOnFail: false,
      },
    ],

    // Máximo 5 % de respuestas superiores a 500 ms.
    slow_response_rate: [
      {
        threshold: 'rate<0.05',
        abortOnFail: false,
      },
    ],
  },

  discardResponseBodies: true,

  summaryTrendStats: [
    'avg',
    'min',
    'med',
    'max',
    'p(90)',
    'p(95)',
    'p(99)',
    'p(99.9)',
  ],
};

export default function () {
  // Distribuye las peticiones entre todas las IP.
  const ip = ips[(__VU + __ITER) % ips.length];

  const response = http.get(
    `http://host.docker.internal:8080/api/v1/ips/${ip}`,
    {
      headers: {
        Accept: 'application/json',
        Connection: 'keep-alive',
      },

      // Tiempo máximo de espera por petición.
      timeout: '30s',

      tags: {
        name: 'GET /api/v1/ips/:ip',
      },
    }
  );

  lookupDuration.add(response.timings.duration);

  const validStatus = response.status === 200;
  const slowResponse = response.timings.duration >= 500;

  check(response, {
    'estado HTTP es 200': () => validStatus,
    'respuesta menor a 500 ms': (res) =>
      res.timings.duration < 500,
    'respuesta menor a 2 segundos': (res) =>
      res.timings.duration < 2000,
  });

  if (validStatus) {
    successfulRequests.add(1);
  } else {
    failedRequests.add(1);

    switch (response.status) {
      case 0:
        connectionErrors.add(1);
        break;

      case 429:
        status429.add(1);
        break;

      case 500:
        status500.add(1);
        break;

      case 503:
        status503.add(1);
        break;

      default:
        unexpectedStatuses.add(1);
    }
  }

  // No clasifica una respuesta lenta como error funcional.
  functionalErrorRate.add(!validStatus);

  // La lentitud se registra en una métrica independiente.
  slowResponseRate.add(slowResponse);
}