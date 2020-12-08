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
        <td><p>matchId=&lt;obtained from Individuals Matching API. example: 57072660-1df9-4aeb-b4ea-cd2d7f96e430&gt;</p><p>startDate=2018-01-01</p><p>endDate=2019-03-01</p></td>
        <td><p>200 (OK)</p><p>Payload as response example above</p></td>
    </tr>
    <tr>
        <td>Missing matchId</td>
        <td>matchId query parameter missing</td>
        <td><p>400 (Bad Request)</p>
        <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;matchId is required&quot; }</p>
        </td>
    </tr>
    <tr>
        <td>Missing startDate</td>
        <td>startDate query parameter missing</td>
        <td><p>400 (Bad Request)</p>
        <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;startDate is required&quot; }</p>
        </td>
    </tr>
    <tr>
         <td><p>endDate earlier than startDate</p></td>
         <td><p>Any valid dates where endDate is earlier than startDate</p>
         <p>e.g. startDate=2017-01-01 endDate=2016-01-01</p></td>
         <td><p>400 (Bad Request)</p>
         <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;Invalid time period requested&quot; }</p></td>
    </tr>
    <tr>
         <td>From date requested is earlier than available data</td>
         <td>
           <p>startDate earlier than 31st March 2013</p>
           <p>e.g. startDate=2013-02-28</p>
         </td>
         <td>
           <p>400 (Bad Request)</p>
           <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;startDate earlier than 31st March 2013&quot; }</p>
         </td>
    </tr>
    <tr>
         <td><p>Invalid date format</p></td>
         <td><p>Any date that is not ISO 8601 Extended format</p>
         <p>e.g. 20170101</p></td>
         <td><p>400 (Bad Request)</p>
         <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;startDate: invalid date format&quot; }</p>
         <p>{ &quot;code&quot; : &quot;INVALID_REQUEST&quot;,<br/>&quot;message&quot; : &quot;endDate: invalid date format&quot; }</p></td>
    </tr>
    <tr>
        <td><p>Incorrect matchId</p></td>
        <td><p>The matchId is not valid</p></td>
        <td><p>404 (Not Found)</p>
        <p>{ &quot;code&quot; : &quot;NOT_FOUND&quot;,<br/>&quot;message&quot; : &quot;The resource cannot be found&quot; }</p></td>
    </tr>
    </tbody>
</table>