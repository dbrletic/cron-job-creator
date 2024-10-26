document.getElementById('regressionForm').addEventListener('submit', function(event) {
    event.preventDefault();

    const scheduleData = document.getElementById('scheduleData').value;
    const lines = scheduleData.trim().split('\n').filter(line => line.trim() !== '');;
    const jobs = [];

    for (let i = 0; i < lines.length; i += 4) {
        if (lines[i]) {
            const job = {
                jobName: lines[i],
                additionalInfo: lines[i + 1] || '',
                schedule: lines[i + 2],
                description: lines[i + 3]
            };
            jobs.push(job);
        }
    }

    // Send the jobs data to the Quarkus API
    fetch('/ffe-cronjob/mass-update', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(jobs)
    })
    .then(response => {
        if (!response.ok) {
        throw new Error('Network response was not ok');
    }
    return response.blob(); // Convert the response to a Blob
     })
    .then(blob => {
        // Create a link element to download the ZIP file
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
         a.href = url;
        a.download = 'file.zip'; // Specify the filename for download
        document.body.appendChild(a);
        a.click(); // Programmatically click the link to trigger the download
        a.remove(); // Clean up the link element
        URL.revokeObjectURL(url); // Release the object URL
    })
    .catch(error => {
        console.error('There was a problem with the fetch operation:', error);
     });
 });