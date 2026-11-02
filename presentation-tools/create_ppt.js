const pptxgen = require('pptxgenjs');
const path = require('path');

const pptx = new pptxgen();
pptx.layout = 'LAYOUT_WIDE';
pptx.author = 'Digital Banking System';
pptx.subject = 'Source-grounded project presentation';
pptx.title = 'Digital Banking System';
pptx.company = 'ApexBank';
pptx.lang = 'en-US';
pptx.theme = {
  headFontFace: 'Aptos Display',
  bodyFontFace: 'Aptos',
  lang: 'en-US'
};
pptx.defineSlideMaster({
  title: 'MASTER',
  background: { color: '0B1220' },
  objects: [
    { rect: { x: 0, y: 0, w: 13.333, h: 0.12, fill: { color: '16C79A' }, line: { color: '16C79A' } } },
    { text: { text: 'DIGITAL BANKING SYSTEM', options: { x: 0.55, y: 7.15, w: 5.8, h: 0.2, fontFace: 'Aptos', fontSize: 8, color: '7F8EA8', margin: 0, breakLine: false } } },
    { text: { text: 'APEXBANK', options: { x: 11.3, y: 7.15, w: 1.45, h: 0.2, fontFace: 'Aptos', fontSize: 8, bold: true, color: '16C79A', align: 'right', margin: 0 } } }
  ],
  slideNumber: { x: 12.85, y: 0.28, color: '7F8EA8', fontFace: 'Aptos', fontSize: 8 }
});

const root = path.resolve(__dirname, '..');
const img = (name) => path.join(root, name);
const C = { bg: '0B1220', panel: '141F33', panel2: '1B2940', text: 'F5F7FA', muted: '9BAAC0', teal: '16C79A', gold: 'F2B84B', red: 'EF6A6A', blue: '68A7FF' };

function title(slide, heading, sub) {
  slide.addText(heading, { x: 0.6, y: 0.42, w: 8.6, h: 0.55, fontFace: 'Aptos Display', fontSize: 26, bold: true, color: C.text, margin: 0 });
  if (sub) slide.addText(sub, { x: 0.63, y: 1.02, w: 10.8, h: 0.28, fontSize: 11, color: C.muted, margin: 0 });
}
function bullet(slide, text, x, y, w, color = C.text, size = 16) {
  slide.addText(text, { x, y, w, h: 0.38, fontSize: size, color, breakLine: false, margin: 0, bullet: { type: 'ul' }, paraSpaceAfterPt: 8 });
}
function panel(slide, x, y, w, h, fill = C.panel) {
  slide.addShape(pptx.ShapeType.roundRect, { x, y, w, h, rectRadius: 0.06, fill: { color: fill }, line: { color: '263852', width: 1 } });
}
function label(slide, text, x, y, w, color = C.teal) {
  slide.addText(text.toUpperCase(), { x, y, w, h: 0.22, fontSize: 9, bold: true, charSpacing: 1.1, color, margin: 0 });
}
function code(slide, text, x, y, w, h, fontSize = 12) {
  slide.addText(text, { x, y, w, h, fontFace: 'Consolas', fontSize, color: 'D5E4F4', fill: { color: '0A1020' }, margin: 0.16, breakLine: false, fit: 'shrink' });
}
function addNotes(slide, text) {
  if (typeof slide.addNotes === 'function') slide.addNotes(text);
}

// 1
{
  const s = pptx.addSlide('MASTER');
  s.background = { color: C.bg };
  s.addShape(pptx.ShapeType.rect, { x: 0, y: 0, w: 13.333, h: 7.5, fill: { color: C.bg }, line: { color: C.bg } });
  s.addShape(pptx.ShapeType.arc, { x: 8.5, y: -1.15, w: 5.2, h: 5.2, adjustPoint: 0.28, line: { color: C.teal, transparency: 35, width: 3 }, fill: { color: C.bg, transparency: 100 } });
  s.addText('DIGITAL BANKING\nSYSTEM', { x: 0.7, y: 1.05, w: 7.2, h: 1.5, fontFace: 'Aptos Display', fontSize: 38, bold: true, color: C.text, margin: 0, breakLine: false, fit: 'shrink' });
  s.addText('A source-grounded technical presentation', { x: 0.75, y: 2.8, w: 5.9, h: 0.35, fontSize: 18, color: C.teal, margin: 0 });
  s.addText('React + Spring Boot microservices\nOTP/JWT security | PostgreSQL | Redis | Kafka', { x: 0.75, y: 3.55, w: 6.5, h: 0.75, fontSize: 16, color: C.muted, breakLine: false, margin: 0, fit: 'shrink' });
  panel(s, 8.15, 3.2, 4.3, 1.6, C.panel);
  s.addText('APEXBANK', { x: 8.55, y: 3.58, w: 3.5, h: 0.45, fontSize: 26, bold: true, color: C.teal, align: 'center', margin: 0 });
  s.addText('Actual implementation, not a generic model', { x: 8.55, y: 4.18, w: 3.5, h: 0.28, fontSize: 11, color: C.muted, align: 'center', margin: 0 });
  addNotes(s, 'Introduce the project as a working microservice prototype. Emphasize that this presentation follows the actual source code and also identifies limitations.');
}

// 2 screenshot registration
{
  const s = pptx.addSlide('MASTER'); title(s, 'Real UI: Create Account', 'Captured from the running Vite frontend at /register');
  panel(s, 0.6, 1.45, 7.7, 5.25, C.panel2);
  s.addImage({ path: img('register-full.png'), x: 0.82, y: 1.65, w: 7.25, h: 4.75 });
  panel(s, 8.75, 1.45, 3.9, 5.25, C.panel);
  label(s, 'Observed screen', 9.1, 1.85);
  bullet(s, 'ApexBank registration form', 9.1, 2.35, 3.1, C.text, 15);
  bullet(s, 'Username and password', 9.1, 2.85, 3.1, C.text, 15);
  bullet(s, 'Email and phone', 9.1, 3.35, 3.1, C.text, 15);
  bullet(s, 'Branch ID and employee code', 9.1, 3.85, 3.1, C.text, 15);
  bullet(s, 'Routes to /user/register through gateway', 9.1, 4.45, 3.1, C.teal, 15);
  s.addText('This is the actual project UI, not a mock screenshot.', { x: 9.1, y: 5.55, w: 3.05, h: 0.7, fontSize: 13, bold: true, color: C.gold, margin: 0, fit: 'shrink' });
  addNotes(s, 'This screenshot proves the frontend is running. The form fields come from the actual React page.');
}

// 3 screenshot login
{
  const s = pptx.addSlide('MASTER'); title(s, 'Real UI: Secure Login', 'Captured from the running Vite frontend at /login');
  panel(s, 0.6, 1.45, 7.7, 5.25, C.panel2);
  s.addImage({ path: img('login-full.png'), x: 0.82, y: 1.65, w: 7.25, h: 4.75 });
  panel(s, 8.75, 1.45, 3.9, 5.25, C.panel);
  label(s, 'Observed screen', 9.1, 1.85);
  bullet(s, 'ApexBank Portal', 9.1, 2.35, 3.1, C.text, 15);
  bullet(s, 'Username/password input', 9.1, 2.85, 3.1, C.text, 15);
  bullet(s, 'Request OTP action', 9.1, 3.35, 3.1, C.teal, 15);
  bullet(s, 'Registration link', 9.1, 3.85, 3.1, C.text, 15);
  bullet(s, 'Login calls auth-service through gateway', 9.1, 4.45, 3.1, C.text, 15);
  s.addText('Protected dashboard states require valid demo credentials.', { x: 9.1, y: 5.55, w: 3.05, h: 0.7, fontSize: 13, bold: true, color: C.gold, margin: 0, fit: 'shrink' });
  addNotes(s, 'Explain that the frontend first requests an OTP. The next token step is handled by the auth service OAuth2 endpoint.');
}

// 4 architecture
{
  const s = pptx.addSlide('MASTER'); title(s, 'Actual Architecture', 'Separate Spring Boot applications, separate persistence boundaries');
  panel(s, 0.65, 1.5, 2.15, 1.0, C.panel2); s.addText('React / Vite\n:5173', { x: 0.85, y: 1.78, w: 1.75, h: 0.45, align: 'center', fontSize: 17, bold: true, color: C.text, margin: 0 });
  panel(s, 3.35, 1.5, 2.3, 1.0, '17324A'); s.addText('API Gateway\n:8080', { x: 3.58, y: 1.78, w: 1.85, h: 0.45, align: 'center', fontSize: 17, bold: true, color: C.text, margin: 0 });
  const services = [['Auth', 'authdb', 0.8, 3.2, C.teal], ['Customer', 'customerdb', 3.05, 3.2, C.blue], ['Account', 'accountdb', 5.3, 3.2, C.gold], ['Transaction', 'transactiondb', 7.55, 3.2, C.red]];
  services.forEach(([a,b,x,y,c]) => { panel(s, x, y, 2.0, 1.15, C.panel); s.addText(`${a} Service`, { x:x+0.12,y:y+0.2,w:1.76,h:0.25,fontSize:16,bold:true,color:c,align:'center',margin:0 }); s.addText(`${b}\nPostgreSQL`, { x:x+0.12,y:y+0.62,w:1.76,h:0.3,fontSize:11,color:C.muted,align:'center',margin:0 }); });
  s.addShape(pptx.ShapeType.line, { x: 2.8, y: 2.0, w: 0.55, h: 0, line: { color: C.teal, width: 2, beginArrowType: 'none', endArrowType: 'triangle' } });
  s.addShape(pptx.ShapeType.line, { x: 4.5, y: 2.5, w: 0, h: 0.65, line: { color: C.teal, width: 2, endArrowType: 'triangle' } });
  s.addText('Eureka :8761', { x: 9.95, y: 1.65, w: 2.2, h: 0.35, fontSize: 17, bold: true, color: C.teal, align: 'center', margin: 0 });
  s.addText('Config Server :8888', { x: 9.75, y: 2.12, w: 2.6, h: 0.3, fontSize: 14, color: C.muted, align: 'center', margin: 0 });
  s.addText('Redis: OTP / cache / locks', { x: 9.55, y: 3.6, w: 2.9, h: 0.3, fontSize: 14, color: C.teal, align: 'center', margin: 0 });
  s.addText('Kafka + Zookeeper: events', { x: 9.55, y: 4.05, w: 2.9, h: 0.3, fontSize: 14, color: C.gold, align: 'center', margin: 0 });
  s.addText('Synchronous service calls use OpenFeign/HTTP. Kafka is producer-only in the confirmed source path.', { x: 0.9, y: 5.55, w: 8.7, h: 0.45, fontSize: 16, color: C.muted, margin: 0, fit: 'shrink' });
  addNotes(s, 'Walk left to right. The gateway is the public entry point. The four business services own separate databases.');
}

// 5 technology and service map
{
  const s = pptx.addSlide('MASTER'); title(s, 'Services and Technology Map', 'The repository contains business services plus platform services');
  const rows = [
    ['Auth', 'Users, roles, password, OTP, JWT', 'Spring Security, OAuth2, Redis, PostgreSQL'],
    ['Customer', 'Profile and KYC', 'JPA, PostgreSQL, WebClient/Setu'],
    ['Account', 'Account numbers and balances', 'JPA, Flyway, Redis locks, Kafka'],
    ['Transaction', 'Transfer, history, audit, reversal', 'JPA, OpenFeign, Resilience4j, Kafka'],
    ['Gateway', 'Routing, edge security, logging', 'Spring Cloud Gateway, JWT resource server'],
    ['Eureka / Config', 'Discovery and configuration', 'Spring Cloud Netflix / Config Server']
  ];
  rows.forEach((r,i) => { const y=1.55+i*0.78; panel(s,0.7,y,2.05,0.58,C.panel2); s.addText(r[0],{x:0.85,y:y+0.17,w:1.75,h:0.22,fontSize:15,bold:true,color:C.teal,align:'center',margin:0}); panel(s,2.95,y,4.15,0.58,C.panel); s.addText(r[1],{x:3.15,y:y+0.17,w:3.75,h:0.22,fontSize:14,color:C.text,margin:0,fit:'shrink'}); panel(s,7.3,y,5.25,0.58,C.panel); s.addText(r[2],{x:7.5,y:y+0.17,w:4.85,h:0.22,fontSize:13,color:C.muted,margin:0,fit:'shrink'}); });
  label(s, 'Root modules', 0.75, 6.45, 2.0); s.addText('auth-service | customer-service | account-service | transaction-service | api-gateway | eureka-server | config-server | frontend', { x: 2.25, y: 6.42, w: 10.0, h: 0.25, fontSize: 12, color: C.text, margin: 0, fit: 'shrink' });
}

// 6 registration/login flow
{
  const s = pptx.addSlide('MASTER'); title(s, 'Registration and Login Flow', 'Source path: frontend -> gateway -> auth-service');
  panel(s,0.65,1.5,5.85,4.85,C.panel); label(s,'Registration',0.95,1.85); code(s,'POST /api/auth/register\n  -> gateway rewrite: /user/register\n  -> UserController.register()\n  -> UserService.register()\n  -> BCrypt encode\n  -> UserRepository.save()\n  -> authdb\n  -> response: user id + username',0.95,2.25,5.25,3.2,15);
  panel(s,6.8,1.5,5.85,4.85,C.panel); label(s,'Login + OTP',7.1,1.85,C.gold); code(s,'POST /api/auth/login\n  -> LoginController.login()\n  -> BCrypt check\n  -> OtpService.generateOtp()\n  -> Redis, 30-minute TTL\n  -> SMS/email/local log\n  -> POST /oauth2/token\n  -> OtpAuthenticationProvider\n  -> signed JWT',7.1,2.25,5.25,3.2,15);
  s.addText('Registration creates an auth user only; Customer and Account are separate later steps.',{x:0.9,y:6.6,w:11.6,h:0.28,fontSize:15,bold:true,color:C.gold,align:'center',margin:0});
}

// 7 JWT/security
{
  const s = pptx.addSlide('MASTER'); title(s, 'JWT and Authorization', 'The token carries identity and authorities across the services');
  panel(s,0.7,1.55,5.55,4.9,C.panel2); label(s,'Token creation',1.0,1.9); code(s,'OtpAuthenticationConverter\n        |\nOtpAuthenticationProvider\n        |\nAuthorizationServerConfig\n        |\nJwkConfig: RSA 2048 key\n        |\nJWT claims:\n  userId\n  authorities\n  standard OAuth2 claims',1.0,2.35,4.9,3.45,16);
  panel(s,6.65,1.55,5.95,4.9,C.panel); label(s,'Token usage',6.95,1.9,C.blue); code(s,'Axios interceptor\n  Authorization: Bearer <JWT>\n        |\nGateway validates JWT\n        |\nService SecurityFilterChain\n        |\nJwtAuthenticationConverter\n        |\nSecurityUtil.getUserId()\n        |\n@PreAuthorize authorities',6.95,2.35,5.25,3.45,16);
  s.addText('Important implementation detail: authorities are read from the custom authorities claim with no added prefix.',{x:0.95,y:6.65,w:11.6,h:0.25,fontSize:14,color:C.teal,align:'center',margin:0});
}

// 8 customer/account
{
  const s = pptx.addSlide('MASTER'); title(s, 'Customer and Account Creation', 'Ownership is established through JWT userId -> customerId -> account.customerId');
  panel(s,0.7,1.55,5.8,4.8,C.panel); label(s,'Customer profile',1.0,1.9); code(s,'POST /api/customers\n  -> CustomerController.create()\n  -> CustomerService.create()\n  -> SecurityUtil.getUserId()\n  -> prevent duplicate profile\n  -> CustomerRepository.save()\n  -> customerdb',1.0,2.35,5.2,3.15,16);
  panel(s,6.8,1.55,5.8,4.8,C.panel); label(s,'Bank account',7.1,1.9,C.gold); code(s,'POST /api/accounts\n  -> AccountController.create()\n  -> CREATE_ACCOUNT\n  -> AccountService.accountCreate()\n  -> CustomerClient\n  -> account_seq\n  -> AccountNumberGenerator\n  -> AccountRepository.save()\n  -> accountdb',7.1,2.35,5.2,3.15,16);
}

// 9 database
{
  const s = pptx.addSlide('MASTER'); title(s, 'Database Boundaries', 'Each service owns its persistence; cross-service links are logical IDs');
  const dbs = [
    ['authdb','users\nroles / permissions\nuser_roles / role_permissions',C.teal],
    ['customerdb','customers\nkyc_details',C.blue],
    ['accountdb','accounts\naccount_seq',C.gold],
    ['transactiondb','transactions\ntransaction_audit',C.red]
  ];
  dbs.forEach((d,i)=>{const x=0.65+i*3.15;panel(s,x,1.65,2.65,2.2,C.panel);s.addText(d[0],{x:x+0.2,y:1.95,w:2.25,h:0.32,fontSize:20,bold:true,color:d[2],align:'center',margin:0});s.addText(d[1],{x:x+0.25,y:2.6,w:2.15,h:0.75,fontFace:'Consolas',fontSize:14,color:C.text,align:'center',margin:0});});
  s.addText('Logical references', {x:0.9,y:4.55,w:2.0,h:0.3,fontSize:17,bold:true,color:C.teal,margin:0});
  code(s,'auth.users.id  --(userId)--> customer.Customer.userId\ncustomer.Customer.id --(customerId)--> account.Account.customerId\naccount.Account.accountNumber --(string)--> transaction.Transaction.accountNumber',0.9,5.0,11.7,1.1,15);
}

// 10 transfer flow
{
  const s = pptx.addSlide('MASTER'); title(s, 'Money Transfer: Actual Execution', 'The happy path is implemented, but the commit is not distributed');
  const steps = [
    '1  Validate amount and account numbers',
    '2  Load sender through AccountClient',
    '3  Verify sender belongs to JWT customer',
    '4  Check balance and blocked status',
    '5  Load receiver and calculate target balances',
    '6  Direct sender update through internal endpoint',
    '7  Direct receiver update through internal endpoint',
    '8  Save DEBIT + CREDIT + audit + Kafka attempt'
  ];
  steps.forEach((t,i)=>{const col=i<4?0:1;const row=i%4;const x=0.8+col*6.1;const y=1.55+row*1.05;panel(s,x,y,5.55,0.75,i===5? '3B2731':C.panel);s.addText(t,{x:x+0.22,y:y+0.22,w:5.1,h:0.25,fontSize:15,color:i===5?C.red:C.text,margin:0,fit:'shrink'});});
  s.addText('Controller: TransactionController.transfer()   Service: TransactionService.transfer()', {x:0.85,y:6.3,w:11.6,h:0.3,fontSize:15,bold:true,color:C.teal,align:'center',margin:0});
  addNotes(s, 'Point out that transaction-service @Transactional cannot roll back account-service writes.');
}

// 11 consistency
{
  const s = pptx.addSlide('MASTER'); title(s, 'Transfer Consistency: Honest Assessment', 'The most important technical limitation in the project');
  panel(s,0.7,1.55,5.75,4.95,C.panel); label(s,'What works',1.0,1.9,C.teal); bullet(s,'Sender ownership check',1.0,2.35,4.9,C.text,16); bullet(s,'Positive amount and same-account checks',1.0,2.8,4.9,C.text,16); bullet(s,'Insufficient balance check before write',1.0,3.25,4.9,C.text,16); bullet(s,'Debit and credit transaction rows',1.0,3.7,4.9,C.text,16); bullet(s,'Best-effort compensation attempt',1.0,4.15,4.9,C.text,16);
  panel(s,6.75,1.55,5.85,4.95,'3B2731'); label(s,'What is not guaranteed',7.05,1.9,C.red); bullet(s,'No distributed transaction',7.05,2.35,5.0,C.text,16); bullet(s,'Direct controller write bypasses lock path',7.05,2.8,5.0,C.text,16); bullet(s,'Concurrent stale target balances can race',7.05,3.25,5.0,C.text,16); bullet(s,'Compensation can fail',7.05,3.7,5.0,C.text,16); bullet(s,'No client idempotency key for /transfer',7.05,4.15,5.0,C.text,16);
  s.addText('Source comparison: TransactionService.transfer() vs AccountController.updateBalance() vs AccountService.updateBalance()', {x:0.85,y:6.65,w:11.7,h:0.24,fontSize:12,color:C.gold,align:'center',margin:0,fit:'shrink'});
}

// 12 history and events
{
  const s = pptx.addSlide('MASTER'); title(s, 'History, Audit, and Events', 'What is persisted after the banking operation');
  panel(s,0.7,1.55,5.7,4.9,C.panel2); label(s,'Transaction history',1.0,1.9); code(s,'GET /api/transactions/me\n  -> AccountClient.getMyAccounts()\n  -> verify caller account numbers\n  -> TransactionRepository\n  -> order by createAt desc\n\nTransfer creates:\n  DEBIT row for sender\n  CREDIT row for receiver',1.0,2.35,5.1,3.35,15);
  panel(s,6.7,1.55,5.9,4.9,C.panel); label(s,'Audit and Kafka',7.0,1.9,C.gold); code(s,'TransactionAuditService.log()\n  txnId, userId, action, IP, deviceId\n\nKafka producers:\n  account-created-topic\n  transaction-event\n\nNo Kafka consumers were found\nin the confirmed source path.',7.0,2.35,5.3,3.35,15);
}

// 13 frontend/backend proof
{
  const s = pptx.addSlide('MASTER'); title(s, 'Frontend-to-Backend Contract', 'The UI uses Axios and gateway paths; some field mismatches remain');
  panel(s,0.7,1.55,5.65,4.95,C.panel); label(s,'Frontend files',1.0,1.9); code(s,'src/App.jsx\nsrc/context/AuthContext.jsx\nsrc/api/axiosClient.js\nsrc/api/authApi.js\nsrc/api/customerApi.js\nsrc/api/accountApi.js\nsrc/api/transactionApi.js\nsrc/pages/LoginPage.jsx\nsrc/pages/RegisterPage.jsx\nsrc/pages/AccountsPage.jsx\nsrc/pages/TransactionsPage.jsx',1.0,2.3,5.05,3.65,14);
  panel(s,6.65,1.55,5.95,4.95,'3B2731'); label(s,'Verify before demo',6.95,1.9,C.gold); bullet(s,'Account response: backend type vs UI accountType',6.95,2.4,5.1,C.text,15); bullet(s,'KYC: aadharNumber vs aadhaarNumber',6.95,3.1,5.1,C.text,15); bullet(s,'Transfer response: backend txnId vs UI fallback fields',6.95,3.8,5.1,C.text,15); bullet(s,'Protected pages need valid seeded credentials',6.95,4.5,5.1,C.text,15);
}

// 14 demo sequence
{
  const s = pptx.addSlide('MASTER'); title(s, 'Live Demonstration Sequence', 'Use disposable data and show the source path as you operate the UI');
  const demo = ['1  Start Docker services + frontend','2  Show Eureka registrations','3  Register user A and user B','4  Login: password -> OTP -> JWT','5  Create customer profiles','6  Create accounts with deposits','7  Show balances','8  Transfer small amount','9  Show debit/credit history','10  Explain failure/consistency caveat'];
  demo.forEach((t,i)=>{const x=i<5?0.85:6.8;const y=1.55+(i%5)*0.88;panel(s,x,y,5.55,0.62,C.panel);s.addText(t,{x:x+0.22,y:y+0.18,w:5.1,h:0.23,fontSize:15,color:i===9?C.gold:C.text,margin:0});});
  s.addText('URLs: frontend http://localhost:5173 | gateway http://localhost:8080 | Eureka http://localhost:8761', {x:0.85,y:6.45,w:11.7,h:0.28,fontSize:14,color:C.teal,align:'center',margin:0});
}

// 15 conclusion
{
  const s = pptx.addSlide('MASTER'); title(s, 'Conclusion and Viva Position', 'A working prototype with clear production-hardening opportunities');
  panel(s,0.75,1.55,7.0,4.8,C.panel2); label(s,'Say this clearly',1.05,1.9); s.addText('"The project demonstrates a complete banking workflow using microservices, JWT/OTP security, separate databases, synchronous OpenFeign calls, and Kafka event publication. The happy-path transfer works, but it is not a distributed atomic transaction. The next engineering step is stronger idempotency, locking, recovery, key management, and integration testing."', {x:1.05,y:2.35,w:6.35,h:2.3,fontSize:22,bold:true,color:C.text,italic:true,margin:0.05,fit:'shrink'});
  panel(s,8.05,1.55,4.55,4.8,C.panel); label(s,'Know these classes',8.35,1.9,C.teal); code(s,'LoginController\nOtpService\nAuthorizationServerConfig\nJwkConfig\nAccountService\nAccountController\nTransactionService\nTransactionController\nAccountClient\nSecurityUtil',8.35,2.35,3.95,3.25,15);
  s.addText('Thank you', {x:8.35,y:5.85,w:3.9,h:0.35,fontSize:24,bold:true,color:C.teal,align:'center',margin:0});
  addNotes(s, 'End by distinguishing confirmed implementation from future improvements. That honesty is a strength in a viva.');
}

pptx.writeFile({ fileName: path.join(root, 'Digital_Banking_System_Presentation.pptx') });
