<#ftl output_format="HTML">
<#import "enter.ftl" as enter/>
<@enter.enter title="Your Jabber ID in the Quicksy directory">
<ol>
      <li>Enter your Jabber ID.</li>
      <li>We will send you a verificaton code to your Jabber ID.</li>
      <li>Pay the one time registration fee of ${fee?string["0.00"]} Euro.¹ <span class="secondary">(This fee is waived for paying customers of the <a href="https://account.conversations.im">conversations.im hosting service</a>.)</spar></li>
      <li>Enter your phone number.</li>
      <li>We will send you a verification code to your phone number.</li>
      <li>If the verification is successful <strong>your Jabber ID will now be discoverable by Quicksy users</strong>.</li>
    </ol>
    <p class="secondary small">¹ The fee will be credited to your Jabber ID. If you abort the process after making the payment but before verifying your phone number you can start over by coming back to this site and re-entering your Jabber ID.</p>
    <p class="continue"><a href="/enter/send-jabber-verification/">Get started</a></p>
</@enter.enter>