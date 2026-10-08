const {test}=require('node:test');
const assert=require('node:assert/strict');
const {SurveyService}=require('../src/services/survey.service');
function serviceFor(requiresRegionVerification, profile) {
  const tx={survey:{async findUnique(){return {id:1,status:'OPEN',requiresRegionVerification,questions:[]}}},
    user:{async findUnique(){return profile}},response:{async findUnique(){return null}}};
  return new SurveyService({$transaction: action=>action(tx)});
}
test('required surveys reject unverified and wrong-region accounts before any writes', async()=>{
  for(const profile of [null,{regionVerifiedAt:null,verifiedRegionCode:'1135056000'},{regionVerifiedAt:new Date(),verifiedRegionCode:'other'}]){
    await assert.rejects(serviceFor(true,profile).submitResponse(2,1,[]),e=>e.status===403 && e.message.includes('지역 인증'));
  }
});
test('verified accounts and unrestricted surveys proceed to normal answer validation',async()=>{
  for(const [required,profile] of [[true,{regionVerifiedAt:new Date(),verifiedRegionCode:'1135056000'}],[false,null]]){
    await assert.rejects(serviceFor(required,profile).submitResponse(2,1,[]),e=>e.status===400 && e.message==='Survey has no questions');
  }
});
