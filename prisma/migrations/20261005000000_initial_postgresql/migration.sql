-- CreateTable
CREATE TABLE "USER" (
    "user_id" SERIAL NOT NULL,
    "email" TEXT NOT NULL,
    "password" TEXT NOT NULL,
    "login_id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "age_group" TEXT,
    "region" TEXT,
    "point" INTEGER NOT NULL DEFAULT 0,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "USER_pkey" PRIMARY KEY ("user_id")
);

-- CreateTable
CREATE TABLE "SURVEY" (
    "survey_id" SERIAL NOT NULL,
    "user_id" INTEGER NOT NULL,
    "title" TEXT NOT NULL,
    "category" TEXT,
    "description" TEXT,
    "audience" TEXT,
    "duration" TEXT,
    "imageData" TEXT,
    "fundedReward" INTEGER NOT NULL DEFAULT 0,
    "reward_point" INTEGER NOT NULL DEFAULT 0,
    "target_count" INTEGER NOT NULL DEFAULT 0,
    "current_count" INTEGER NOT NULL DEFAULT 0,
    "status" TEXT NOT NULL DEFAULT 'OPEN',
    "end_date" TIMESTAMP(3),
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "SURVEY_pkey" PRIMARY KEY ("survey_id")
);

-- CreateTable
CREATE TABLE "QUESTION" (
    "question_id" SERIAL NOT NULL,
    "survey_id" INTEGER NOT NULL,
    "question" TEXT NOT NULL,
    "required" BOOLEAN NOT NULL DEFAULT true,
    "question_type" TEXT NOT NULL DEFAULT 'single',

    CONSTRAINT "QUESTION_pkey" PRIMARY KEY ("question_id")
);

-- CreateTable
CREATE TABLE "SURVEY_OPTION" (
    "option_id" SERIAL NOT NULL,
    "question_id" INTEGER NOT NULL,
    "option_text" TEXT NOT NULL,

    CONSTRAINT "SURVEY_OPTION_pkey" PRIMARY KEY ("option_id")
);

-- CreateTable
CREATE TABLE "RESPONSE" (
    "response_id" SERIAL NOT NULL,
    "user_id" INTEGER NOT NULL,
    "survey_id" INTEGER NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "RESPONSE_pkey" PRIMARY KEY ("response_id")
);

-- CreateTable
CREATE TABLE "ANSWER" (
    "answer_id" SERIAL NOT NULL,
    "response_id" INTEGER NOT NULL,
    "question_id" INTEGER NOT NULL,
    "answer" TEXT NOT NULL,

    CONSTRAINT "ANSWER_pkey" PRIMARY KEY ("answer_id")
);

-- CreateTable
CREATE TABLE "POINT_HISTORY" (
    "history_id" SERIAL NOT NULL,
    "user_id" INTEGER NOT NULL,
    "amount" INTEGER NOT NULL,
    "description" TEXT NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "payment_order_id" TEXT,

    CONSTRAINT "POINT_HISTORY_pkey" PRIMARY KEY ("history_id")
);

-- CreateTable
CREATE TABLE "PAYMENT_ORDER" (
    "id" TEXT NOT NULL,
    "user_id" INTEGER NOT NULL,
    "provider" TEXT NOT NULL,
    "amount" INTEGER NOT NULL,
    "credit_point" INTEGER NOT NULL,
    "status" TEXT NOT NULL DEFAULT 'CREATED',
    "provider_ref" TEXT,
    "checkout_url" TEXT,
    "callback_hash" TEXT NOT NULL,
    "approved_at" TIMESTAMP(3),
    "credited_at" TIMESTAMP(3),
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "PAYMENT_ORDER_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "COUPON" (
    "id" TEXT NOT NULL,
    "user_id" INTEGER NOT NULL,
    "item_id" TEXT NOT NULL,
    "shop" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "cost" INTEGER NOT NULL,
    "status" TEXT NOT NULL DEFAULT 'AVAILABLE',
    "request_key" TEXT NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "used_at" TIMESTAMP(3),

    CONSTRAINT "COUPON_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "USER_email_key" ON "USER"("email");

-- CreateIndex
CREATE UNIQUE INDEX "USER_login_id_key" ON "USER"("login_id");

-- CreateIndex
CREATE UNIQUE INDEX "RESPONSE_user_id_survey_id_key" ON "RESPONSE"("user_id", "survey_id");

-- CreateIndex
CREATE UNIQUE INDEX "POINT_HISTORY_payment_order_id_key" ON "POINT_HISTORY"("payment_order_id");

-- CreateIndex
CREATE INDEX "PAYMENT_ORDER_user_id_created_at_idx" ON "PAYMENT_ORDER"("user_id", "created_at");

-- CreateIndex
CREATE UNIQUE INDEX "PAYMENT_ORDER_provider_provider_ref_key" ON "PAYMENT_ORDER"("provider", "provider_ref");

-- CreateIndex
CREATE INDEX "COUPON_user_id_created_at_idx" ON "COUPON"("user_id", "created_at");

-- CreateIndex
CREATE UNIQUE INDEX "COUPON_user_id_request_key_key" ON "COUPON"("user_id", "request_key");

-- AddForeignKey
ALTER TABLE "SURVEY" ADD CONSTRAINT "SURVEY_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "USER"("user_id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "QUESTION" ADD CONSTRAINT "QUESTION_survey_id_fkey" FOREIGN KEY ("survey_id") REFERENCES "SURVEY"("survey_id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "SURVEY_OPTION" ADD CONSTRAINT "SURVEY_OPTION_question_id_fkey" FOREIGN KEY ("question_id") REFERENCES "QUESTION"("question_id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "RESPONSE" ADD CONSTRAINT "RESPONSE_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "USER"("user_id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "RESPONSE" ADD CONSTRAINT "RESPONSE_survey_id_fkey" FOREIGN KEY ("survey_id") REFERENCES "SURVEY"("survey_id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ANSWER" ADD CONSTRAINT "ANSWER_response_id_fkey" FOREIGN KEY ("response_id") REFERENCES "RESPONSE"("response_id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ANSWER" ADD CONSTRAINT "ANSWER_question_id_fkey" FOREIGN KEY ("question_id") REFERENCES "QUESTION"("question_id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "POINT_HISTORY" ADD CONSTRAINT "POINT_HISTORY_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "USER"("user_id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "POINT_HISTORY" ADD CONSTRAINT "POINT_HISTORY_payment_order_id_fkey" FOREIGN KEY ("payment_order_id") REFERENCES "PAYMENT_ORDER"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "PAYMENT_ORDER" ADD CONSTRAINT "PAYMENT_ORDER_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "USER"("user_id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "COUPON" ADD CONSTRAINT "COUPON_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "USER"("user_id") ON DELETE RESTRICT ON UPDATE CASCADE;

