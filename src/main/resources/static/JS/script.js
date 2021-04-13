const search = () => {
	let query = $("#search-input").val();
	console.log(query);
	if (query == '') {
		$(".search-result").hide();
	} else {
		let url = `http://localhost:1200/search/${query}`;
		fetch(url).then((response) => {
			return response.json();
		}).then((data) => {
			console.log(data);
			let text = `<div class='list-group'>`;
			data.forEach((contact) => {
				text += `<a href='/user/${contact.name}/contact/${contact.cId}' class='list-group-item list-group-item-action'>${contact.name}</a>`
			});
			text += `</div>`;
			$(".search-result").html(text);
			$(".search-result").show();
		});
	}
};


//first request- toserver to create order

const paymentStart = () =>{
console.log("payment Started");
let amount=$("#payment_field").val();
console.log(amount);
if(amount==""||amount==null){
	//alert("amount is required!");
	swal("Amount Missing !","Amount is required !!","error");
	return;
}

$.ajax(
	{
		url: "/user/create_order",
		data:JSON.stringify({
			amount:amount,
			info:'order_request'
		}),
		contentType: "application/json",
		type: "POST",
		dataType:"json",
		success:function(response){
			console.log(response);
			if(response.status=="created"){
				let options={
					key:'rzp_test_uV08rR1BPLKQA1',
					amount:response.amount,
					currency:'INR',
					name:'Smart Contact Manager',
					description:'Donation',
					image:'https://cdn4.vectorstock.com/i/1000x1000/71/28/secure-payment-sign-vector-11807128.jpg',
					order_id:response.id,
					handler:function(response){
						console.log(response.razorpay_payment_id);
						console.log(response.razorpay_order_id);
						console.log(response.razorpay_signature);
						console.log('payment successfully!');
						updatePaymentOnServer(response.razorpay_payment_id,response.razorpay_order_id,"paid");
					},
					prefill:{
						name: "",
						email: "",
						contact: "",
					},
					notes: {
						address : "TDK Development"
					},
					theme:{
						color : "#3399cc"
					},
				};
				let rzp=new Razorpay(options);
				rzp.on("payment.failed",function(response){
					console.log(response.error.code);
					console.log(response.error.description);
					console.log(response.error.source);
					console.log(response.error.step);
					console.log(response.error.reason);
					console.log(response.error.metadata.order_id);
					console.log(response.error.metadata.payment_id);
					swal("Failed !","Payment Failed !!","error");
				});
				rzp.open();
			}
		},
		error:function(error){
			console.log(error);
			alert('Internet not connected / Amount exceeded!!');
		}
	}
)

};

function updatePaymentOnServer(payment_id,order_id,status){
	console.log(payment_id);
	console.log(order_id);
	console.log(status);
	$.ajax(
		{
			url: "/user/update_order",
			data:JSON.stringify({
				payment_id: payment_id,
				order_id: order_id,
				status: status,
			}),
			contentType: "application/json",
			type: "POST",
			dataType:"json",
			success:function(response){
				swal("Thanks for your contribution!","Payment Successfull !!","success");
			},
			error:function(error){
				swal("Failed","Payment successfull ,but we didn't capture it ,we will contact you as soon as possible  !!","error");
			},
		});
}

function myfun(){
		console.log("working");
		 swal("Request Sent Successfully!", {
		      icon: "success",
		      buttons:false
		    });
		    setTimeout(function(){ swal.close(); }, 1000);
	}
	
	