import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 100 },
    { duration: '60s', target: 100 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'], 
    http_reqs: ['count>100']
  },
};

export function setup() {
  // Create user for the load test
  const res = http.post('http://localhost:8081/user/create', JSON.stringify({
    username: 'loaduser_' + Date.now(),
    email: 'loaduser' + Date.now() + '@example.com',
    initialBalance: 1000000.00
  }), { headers: { 'Content-Type': 'application/json' } });

  let userId = 1;
  if (res.status === 200) {
      userId = JSON.parse(res.body).id;
  }
  return { userId: userId };
}

export default function (data) {
  const BASE_URL = 'http://localhost:8081';
  const userId = data.userId || 1;
  
  // 1. Hit deposit endpoint
  const depositPayload = JSON.stringify({ amount: 10.00 });
  const depositHeaders = { 'Content-Type': 'application/json' };
  
  const resDeposit = http.post(`${BASE_URL}/user/${userId}/deposit`, depositPayload, { headers: depositHeaders });
  
  check(resDeposit, {
    'deposit status is 200': (r) => r.status === 200,
  });

  // 2. Hit order endpoint
  const orderPayload = JSON.stringify({
    userId: userId,
    symbol: 'BTCUSDT',
    side: 'BUY',
    quantity: 1
  });
  const orderHeaders = { 
    'Content-Type': 'application/json',
    'Idempotency-Key': `k6-${__VU}-${__ITER}-${Math.random().toString(36).substring(7)}`
  };

  const resOrder = http.post(`${BASE_URL}/api/orders`, orderPayload, { headers: orderHeaders });
  
  check(resOrder, {
    'order status is 201 or 200': (r) => r.status === 201 || r.status === 200,
    'order response contains status': (r) => r.body && r.body.includes('status'),
  });

  sleep(1);
}
