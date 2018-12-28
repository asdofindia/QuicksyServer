<#ftl output_format="HTML">
<#import "enter.ftl" as enter/>
<@enter.enter title="Enter your phone number">
<p>Please enter your phone number including your country code. We will <#if method == "SMS">send you an SMS<#else>call you</#if> with a verification code.</p>
<p><strong>Double check your phone number to make sure it is correct before pressing next.</strong></p>
<#if error??>
  <p class="error">
    <#switch error>
      <#case "invalid">
      The phone number you entered is invalid.
      <p class="secondary small">Use the <a href="https://en.wikipedia.org/wiki/E.164">E.164</a> format. Start with a&nbsp;+. Spaces will be removed automatically.</p>
      <#break>
      <#case "failed">
      Unable to send verification SMS.
      <#break>
      <#case "already">
      You have already verified a phone number. Click <a href="/enter/finished/">here</a>.
      <#break>
      <#default>
      An unknown error has occurred.
    </#switch>
  </p>
</#if>
<form method="post" action="/enter/send-number-verification/">
  <input name="method" type="hidden" value="${method}"/>
  <input name="number" type="text" placeholder="+1 555 443 5222" autocomplete="off"/>
  <p class="continue"><button type="submit">Next</button></p>
</form>
<p class="small secondary"><#if method == "SMS">Click <a href="/enter/send-number-verification/?method=CALL">here</a> to request a phone call instead.<#else>Click <a href="/enter/send-number-verification/?method=SMS">here</a> to request an SMS instead.
</#if></p>
</@enter.enter>
