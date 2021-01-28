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
        <td><p>PAYE employments data found</p></td>
        <td><p>matchId=&lt;obtained from Individuals Matching API. example: 57072660-1df9-4aeb-b4ea-cd2d7f96e430&gt;</p><p>fromDate=2018-01-01</p><p>toDate=2019-03-01</p></td>
        <td><p>200 (OK)</p><p>Payload as response example above</p></td>
    </tr>
    <tr>
        <td><p>Missing matchId</p></td>
        <td><p>matchId query parameter missing</p></td>
        <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;matchId is required&quot; }</p>
        </td>
    </tr>
    <tr>
        <td><p>Missing fromDate</p></td>
        <td><p>fromDate query parameter missing</p></td>
        <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;fromDate is required&quot; }</p>
        </td>
    </tr>
    <tr>
         <td><p>toDate earlier than fromDate</p></td>
         <td><p>Any valid dates where toDate is earlier than fromDate</p>
         <p>e.g. fromDate=2017-01-01 toDate=2016-01-01</p></td>
         <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;Invalid time period requested&quot; }</p>
        </td>
    </tr>
    <tr>
         <td><p>Invalid date format</p></td>
         <td>
            <p>Any date that is not ISO 8601 Extended format</p>
            <p>e.g. 20170101</p>
         </td>
         <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;fromDate: invalid date format&quot; }</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;toDate: invalid date format&quot; }</p>
         </td>
    </tr>
         <td><p>fromDate earlier than the current tax year minus 6</p></td>
         <td>
            <p>For example</p>
            <p>fromDate=2014-01-01</p>
         </td>
         <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;fromDate is earlier than maximum allowed&quot; }</p>
         </td>
    <tr>
    </tr>
    <tr>
        <td><p>Incorrect matchId</p></td>
        <td><p>The matchId is not valid</p></td>
        <td><p>404 (Not Found)</p>
        <p>{ &quot;code&quot; : &quot;NOT_FOUND&quot;,<br/>&quot;message&quot; : &quot;The resource cannot be found&quot; }</p></td>
    </tr>
    <tr>
        <td><p>Missing CorrelationId</p></td>
        <td><p>CorrelationId header is missing</p></td>
        <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;CorrelationId is required&quot; }</p></td>
        </td>
    </tr>
    <tr>
        <td><p>Malformed CorrelationId</p></td>
        <td><p>CorrelationId header is malformed</p></td>
        <td>
            <p>400 (Bad Request)</p>
            <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;Malformed CorrelationId&quot; }</p></td>
        </td>
    </tr>
    </tbody>
</table>