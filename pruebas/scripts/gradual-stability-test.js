import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Carga máxima configurable desde PowerShell.
// Ejemplo: --env MAX_RATE=10000
const MAX_RATE = Number(__ENV.MAX_RATE || 10000);

const REQUEST_TIMEOUT = __ENV.TIMEOUT || '10s';

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

  // DNS y Estados Unidos
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

/*
 * Los targets se calculan como porcentajes del máximo:
 *
 * 10 %  → 1.000 RPS cuando MAX_RATE=10.000
 * 25 %  → 2.500 RPS
 * 50 %  → 5.000 RPS
 * 60 %  → 6.000 RPS
 * 70 %  → 7.000 RPS
 * 80 %  → 8.000 RPS
 * 90 %  → 9.000 RPS
 * 100 % → 10.000 RPS
 */
const rate10 = Math.round(MAX_RATE * 0.10);
const rate25 = Math.round(MAX_RATE * 0.25);
const rate50 = Math.round(MAX_RATE * 0.50);
const rate60 = Math.round(MAX_RATE * 0.60);
const rate70 = Math.round(MAX_RATE * 0.70);
const rate80 = Math.round(MAX_RATE * 0.80);
const rate90 = Math.round(MAX_RATE * 0.90);

export const options = {
  scenarios: {
    gradual_stability: {
      executor: 'ramping-arrival-rate',

      // La prueba comienza con una carga baja.
      startRate: rate10,
      timeUnit: '1s',

      /*
       * Por tus pruebas anteriores, 8.000 RPS llegó a utilizar
       * más de 2.600 VUs. Dejamos un margen mayor.
       */
      preAllocatedVUs: 4000,
      maxVUs: 7000,

      stages: [
        // Calentamiento: 1.000 RPS
        { duration: '30s', target: rate10 },

        // Subir progresivamente a 2.500 RPS
        { duration: '30s', target: rate25 },

        // Subir hasta 5.000 RPS
        { duration: '1m', target: rate50 },

        // Mantener 5.000 RPS
        { duration: '1m', target: rate50 },

        // Subir a 6.000 RPS
        { duration: '30s', target: rate60 },

        // Mantener 6.000 RPS
        { duration: '1m', target: rate60 },

        // Subir a 7.000 RPS
        { duration: '30s', target: rate70 },

        // Mantener 7.000 RPS
        { duration: '1m', target: rate70 },

        // Subir a 8.000 RPS
        { duration: '30s', target: rate80 },

        // Mantener 8.000 RPS
        { duration: '2m', target: rate80 },

        // Subir a 9.000 RPS
        { duration: '30s', target: rate90 },

        // Mantener 9.000 RPS
        { duration: '2m', target: rate90 },

        // Subir a la carga máxima
        { duration: '30s', target: MAX_RATE },

        // Mantener 10.000 RPS
        { duration: '2m', target: MAX_RATE },

        // Recuperación progresiva
        { duration: '30s', target: rate50 },
        { duration: '30s', target: rate10 },
        { duration: '30s', target: 0 },
      ],

      gracefulStop: '30s',

      tags: {
        test_type: 'gradual_stability',
        maximum_rate: String(MAX_RATE),
      },
    },
  },

  thresholds: {
    // Menos del 1 % de errores.
    http_req_failed: ['rate<0.01'],
    functional_error_rate: ['rate<0.01'],

    // Objetivos de latencia.
    http_req_duration: [
      'p(90)<250',
      'p(95)<500',
      'p(99)<1000',
    ],

    ip_lookup_duration: [
      'p(95)<500',
      'p(99)<1000',
    ],

    // Menos del 1 % puede superar 500 ms.
    slow_response_rate: ['rate<0.01'],

    /*
     * Para esta prueba gradual permitimos muy pocos descartes.
     * count==0 sería demasiado estricto para una prueba larga.
     */
    dropped_iterations: ['rate<0.0001'],
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

export function setup() {
  console.log('========================================');
  console.log('PRUEBA GRADUAL DE ESTABILIDAD');
  console.log(`Carga máxima: ${MAX_RATE} RPS`);
  console.log(`Timeout: ${REQUEST_TIMEOUT}`);
  console.log('========================================');

  const response = http.get(
    'http://localhost:8080/api/v1/ips/190.242.44.182',
    {
      timeout: REQUEST_TIMEOUT,
      tags: {
        name: 'precheck',
      },
    }
  );

  if (response.status !== 200) {
    throw new Error(
      `El servicio no superó la validación inicial. HTTP ${response.status}`
    );
  }
}

export default function () {
  const ip = ips[(__VU + __ITER) % ips.length];

  const response = http.get(
    `http://localhost:8080/api/v1/ips/${ip}`,
    {
      headers: {
        Accept: 'application/json',
        Connection: 'keep-alive',
      },

      timeout: REQUEST_TIMEOUT,

      tags: {
        name: 'GET /api/v1/ips/:ip',
      },
    }
  );

  const validStatus = response.status === 200;
  const slowResponse = response.timings.duration >= 500;

  lookupDuration.add(response.timings.duration);
  functionalErrorRate.add(!validStatus);
  slowResponseRate.add(slowResponse);

  check(response, {
    'estado HTTP es 200': () => validStatus,

    'respuesta menor a 500 ms': (res) =>
      res.timings.duration < 500,

    'respuesta menor a 1 segundo': (res) =>
      res.timings.duration < 1000,
  });

  if (validStatus) {
    successfulRequests.add(1);
    return;
  }

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