# YoCabs launch roadmap

Target: limited public launch (one region, e.g. Bhubaneswar - Puri) in about 8-12 weeks.
Tick items off as they finish. Items marked **[start now]** are waiting on third parties, so they set the timeline.

## Phase 0 - Start now (this week, business tasks in parallel with code)

- [ ] **[start now]** Register the business entity (proprietorship / LLP / Pvt Ltd) and get PAN, GST, and a current bank account - needed for Razorpay, Play Console and invoicing
- [ ] **[start now]** Razorpay account and KYC (see the KYC notes below)
- [ ] **[start now]** DLT registration for SMS (entity, sender ID/header, OTP template) with an SMS provider (MSG91, Twilio, etc.)
- [ ] **[start now]** Google Play Console developer account (one-time fee; organisations need a D-U-N-S number)
- [ ] Domain name (for privacy policy, support email, API, admin console)
- [ ] Draft the commercial model: commission %, cancellation and refund rules, partner payout schedule (the backend finance rules must match)

## Phase 1 - Closed beta on the preview app (weeks 1-3)

- [x] Hosted API + Postgres on Railway
- [x] Hosted admin console
- [ ] Preview APK build installed on real phones
- [ ] Add the `EXPO_TOKEN` secret to the YoCabs-Frontend GitHub repo (turns on automatic over-the-air updates)
- [ ] Connect Railway `api` and `admin-web` to GitHub for automatic redeploys
- [ ] Verify the first over-the-air update reaches a phone
- [ ] Real-device QA of the whole journey for all four roles: search, offer, book, pay (sandbox), assign driver, trip, live location, rating, wallet
- [ ] QA of maps, place picker, service-area circles and background location on 2+ different Android phones
- [ ] Restrict the Google Maps key (package `com.yocabs.app` + signing-certificate SHA-1)
- [ ] Onboard 3-5 friendly real partners and 5-10 tourist testers; collect bugs in one list
- [ ] Fix the beta bugs

## Phase 2 - Make it real (weeks 3-8)

Payments
- [ ] Razorpay integration in the backend (replace the sandbox gateway): orders, webhooks with signature check, refunds
- [ ] Payment sheet in the mobile app (`features/payment/launcher.ts`)
- [ ] Reconciliation and payout flow tested with small real amounts

SMS and notifications
- [ ] Real SMS OTP sender (replace the logging sender)
- [ ] Push notifications (Firebase Cloud Messaging via Expo): booking requests, offers, assignments, trip status
- [ ] Booking confirmation SMS / email

Production hardening
- [ ] Separate production environment (own database, own secrets, `prod` profile)
- [ ] Managed Postgres with automated backups and a tested restore
- [ ] Document storage on object storage (S3 / R2); Railway's disk is wiped on redeploy
- [ ] Google Routes API turned on for distance and time (key, budget alert)
- [ ] Production place search (Google Places or similar) instead of Nominatim
- [ ] Monitoring and alerts: uptime check, error tracking (Sentry), log retention
- [ ] Security review: auth, RBAC, rate limits on OTP and login, file upload limits, dependency scan
- [ ] Load test the search and booking endpoints
- [ ] Change all staging passwords and rotate the JWT secret for production

## Phase 3 - Store readiness (weeks 5-9, overlaps Phase 2)

- [ ] Final app icon, adaptive icon, splash, screenshots, feature graphic
- [ ] Privacy policy and terms of service published at a public URL
- [ ] Play Console: app content, Data safety form, target audience, ads declaration
- [ ] Background-location declaration with a video showing the in-app disclosure and the feature (wording must match `useTripSharing`)
- [ ] Production build (`eas build --profile production`), upload the `.aab`
- [ ] Internal testing track, then closed testing (Play requires a closed test with enough testers for a set period before production access; check the current requirement in the Console)
- [ ] Driver-side and partner-side document upload screens (listed as a known gap)

## Phase 4 - Limited launch (weeks 8-12)

- [ ] Support process: phone/email/WhatsApp, who answers, response targets, dispute handling
- [ ] Partner agreements signed; 10-20 operators onboarded in the launch region
- [ ] Launch checklist review with real payments end to end (book, pay, trip, refund, payout)
- [ ] Production rollout to the Play Store (staged rollout, start at 10-20%)
- [ ] Watch dashboards daily for the first two weeks; hot-fix over the air where possible

## Phase 5 - After launch

- [ ] iOS app
- [ ] Tourist-visible driver location
- [ ] Ratings moderation, promotions and coupons, multi-language
- [ ] More regions

---

## Razorpay KYC notes

Do this at razorpay.com. Requirements change, so trust the checklist Razorpay shows you over this summary.

1. Sign up with a business email and mobile number, then verify both.
2. Choose the business type: proprietorship, partnership, LLP, private limited, etc.
3. Business details: legal name, PAN, GSTIN (if registered), registered address, business category (travel and transport).
4. Website or app link: Razorpay reviews a live site or app listing. Have the admin console URL, a simple landing page, or the Play listing ready with your privacy policy, terms and refund policy visible.
5. Authorised signatory: personal PAN, and Aadhaar or another ID for the person signing.
6. Bank account: current account in the business's name (proprietorship can use a savings account in the owner's name); cancelled cheque or bank statement.
7. Submit and wait for review, usually a few working days, longer if they ask for more documents.
8. Marketplace payouts: YoCabs collects a token payment and pays partners later. That is a marketplace/split-payment use case, so ask Razorpay about **Route** (their split-payments product) early; it may need separate approval.
9. Test mode works without KYC, so integration work can start before approval.
