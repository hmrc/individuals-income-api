<table>
    <col width="25%">
    <col width="35%">
    <col width="40%">
    <thead>
    <tr>
        <th>Scenario</th>
        <th>Parameters</th>
        <th>Response</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td><p>A valid, successful request for trusts income data</p></td>
        <td><p>The matchId is obtained from the Individuals Matching API. For example: 57072660-1df9-4aeb-b4ea-cd2d7f96e430</p><p>fromTaxYear=2018-19</p><p>toTaxYear=2019-20</p></td>
        <td><p>200 (OK)</p><p>Payload as response example above</p></td>
    </tr>
    <tr>
        <td><p>Missing matchId</p></td>
        <td><p>The request is missing the matchId. Check the query parameters section for what should be included.</p></td>
        <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;matchId is required&quot; }</p>
        </td>
    </tr>
    <tr>
        <td><p>Missing fromTaxYear</p></td>
        <td><p>The request is missing a fromTaxYear. Check the query parameters section for what should be included.</p></td>
        <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;fromTaxYear is required&quot; }</p>
        </td>
    </tr>
    <tr>
         <td><p>toTaxYear earlier than fromTaxYear</p></td>
         <td><p>Any valid dates where the toTaxYear is earlier than the fromTaxYear.</p><p>For example:</p><p>fromTaxYear=2019-20 toTaxYear=2018-19</p></td>
         <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;Invalid time period requested&quot; }</p>
        m</td>
    </tr>
    <tr>
         <td><p>The toTaxYear is later than the current tax year.</p></td>
         <td><p>For example:</p><p>fromTaxYear=2016-17 toTaxYear=2098-99</p></td>
         <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;toTaxYear is later than the current tax year&quot; }</p>
         </td>
    </tr>
    <tr>
         <td><p>The fromTaxYear is earlier than the current tax year minus 6.</p></td>
         <td><p>For example:</p><p>fromTaxYear=2013-2014</p></td>
         <td>
           <p>400 (Bad Request)</p>
           <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;fromTaxYear earlier than maximum allowed&quot; }</p>
         </td>
    </tr>
    <tr>
         <td><p>Invalid tax year format</p></td>
         <td><p>Any tax year that is not in the correct format. Check the query parameters section for the correct format.</p></td>
         <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;fromTaxYear: invalid tax year format&quot; }</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;toTaxYear: invalid tax year format&quot; }</p>
         </td>
    </tr>
    <tr>
        <td><p>No data found for the provided matchId</p></td>
        <td><p>The matchId has no related data.</p></td>
        <td>
            <p>404 (Not Found)</p>
            <p>{ &quot;code&quot; : &quot;NOT_FOUND&quot;,<br/>&quot;message&quot; : &quot;The resource cannot be found&quot; }</p>
        </td>
    </tr>
    <tr>
        <td><p>Missing CorrelationId</p></td>
        <td><p>The correlationId is missing. Check the request headers section for what should be included.</p></td>
        <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;CorrelationId is required&quot; }</p></td>
        </td>
    </tr>
    <tr>
        <td><p>Malformed CorrelationId</p></td>
        <td><p>The correlationId is in the incorrect format. Check the request headers section for the correct format.</p></td>
        <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;Malformed CorrelationId&quot; }</p></td>
        </td>
    </tr>
    </tbody>
</table>
