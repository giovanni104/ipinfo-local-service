import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Permite modificar la carga desde PowerShell:
// --env RATE=5000
const TARGET_RATE = Number(__ENV.RATE || 5000);

// Duración sostenida de cada nivel.
// Puede cambiarse con: --env DURATION=10m
const TEST_DURATION = __ENV.DURATION || '5m';

// Tiempo máximo de cada petición.
// Para este endpoint no conviene esperar 30 segundos.
const REQUEST_TIMEOUT = __ENV.TIMEOUT || '10s';

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

// IP verificadas previamente con respuesta HTTP 200.
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
    stability: {
      executor: 'constant-arrival-rate',

      // Solicitudes por segundo.
      rate: TARGET_RATE,
      timeUnit: '1s',

      // Mantiene el nivel sin rampas.
      duration: TEST_DURATION,

      /*
       * Basado en los resultados anteriores:
       * - p95 alrededor de 300 ms.
       * - Para 8.000 RPS podrían requerirse más de 2.400 VUs
       *   durante los periodos de mayor latencia.
       */
      preAllocatedVUs: 3000,
      maxVUs: 5000,

      gracefulStop: '30s',

      tags: {
        test_type: 'stability',
        target_rate: String(TARGET_RATE),
      },
    },
  },

  thresholds: {
    // Disponibilidad
    http_req_failed: ['rate<0.01'],
    functional_error_rate: ['rate<0.01'],

    // Rendimiento
    http_req_duration: [
      'p(90)<250',
      'p(95)<500',
      'p(99)<1000',
    ],

    ip_lookup_duration: [
      'p(95)<500',
      'p(99)<1000',
    ],

    // Menos del 1 % puede superar los 500 ms.
    slow_response_rate: ['rate<0.01'],

    /*
     * Para considerar estable un nivel no debería descartarse
     * ninguna solicitud programada.
     */
    dropped_iterations: ['count==0'],
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
  console.log(`Prueba de estabilidad: ${TARGET_RATE} RPS`);
  console.log(`Duración: ${TEST_DURATION}`);
  console.log(`Timeout: ${REQUEST_TIMEOUT}`);
  console.log('========================================');

  // Verificación previa antes de generar la carga.
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
  /*
   * Distribución determinística para recorrer todas las IP,
   * evitando depender únicamente de selección aleatoria.
   */
  const index = (__VU + __ITER) % ips.length;
  const ip = ips[index];

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
        target_rate: String(TARGET_RATE),
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